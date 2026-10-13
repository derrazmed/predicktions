package com.ven.predicktions.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ven.predicktions.model.League;
import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.MatchStatus;
import com.ven.predicktions.model.Prediction;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.LeagueMemberRepository;
import com.ven.predicktions.repository.LeagueRepository;
import com.ven.predicktions.repository.MatchRepository;
import com.ven.predicktions.repository.PredictionRepository;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.PredictionRecalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.IsoFields;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LeagueComparisonControllerIntegrationTest {

    private static final Instant COMPLETED_KICKOFF = Instant.parse("2026-03-02T18:00:00Z");
    private static final Instant INCOMPLETE_KICKOFF = Instant.parse("2026-03-09T18:00:00Z");
    private static final Instant NO_RESULT_KICKOFF = Instant.parse("2026-03-16T18:00:00Z");
    private static final Instant UNSCORED_KICKOFF = Instant.parse("2026-03-23T18:00:00Z");
    private static final Instant UNLOCK_KICKOFF = Instant.parse("2026-03-30T18:00:00Z");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeagueRepository leagueRepository;

    @Autowired
    private LeagueMemberRepository leagueMemberRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private PredictionRecalculationService predictionRecalculationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User viewer;
    private User alice;
    private User bob;
    private User outsider;
    private League league;

    @BeforeEach
    void setUp() {
        predictionRepository.deleteAll();
        leagueMemberRepository.deleteAll();
        leagueRepository.deleteAll();
        matchRepository.deleteAll();
        userRepository.deleteAll();

        viewer = createUser("viewer-" + UUID.randomUUID());
        alice = createUser("alice-" + UUID.randomUUID());
        bob = createUser("bob-" + UUID.randomUUID());
        outsider = createUser("outsider-" + UUID.randomUUID());

        league = new League("League A", viewer, randomJoinCode());
        league.addMember(alice);
        league.addMember(bob);
        league = leagueRepository.save(league);
    }

    @Test
    void completedGameweekRevealsAllMemberPredictions() throws Exception {
        Match match = createMatch("gw-complete", COMPLETED_KICKOFF, MatchStatus.FINISHED, 2, 1);
        createPrediction(viewer, match, 2, 1, 3);
        createPrediction(alice, match, 3, 0, 1);
        createPrediction(bob, match, 0, 2, 0);

        String token = loginAndGetToken(viewer.getUsername());

        ResponseEntity<String> listResponse = get(token, gameweeksPath());

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode gameweeks = objectMapper.readTree(listResponse.getBody());
        assertThat(gameweeks).hasSize(1);
        assertThat(gameweeks.get(0).get("season").asInt()).isEqualTo(season(COMPLETED_KICKOFF));
        assertThat(gameweeks.get(0).get("gameweek").asInt()).isEqualTo(isoWeek(COMPLETED_KICKOFF));
        assertThat(gameweeks.get(0).get("matchCount").asInt()).isEqualTo(1);
        assertThat(gameweeks.get(0).get("label").asText()).isNotBlank();

        ResponseEntity<String> detailResponse = get(
                token, gameweekPath(COMPLETED_KICKOFF));
        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode body = objectMapper.readTree(detailResponse.getBody());
        assertThat(body.get("leagueId").asText()).isEqualTo(league.getId().toString());
        assertThat(body.get("members")).hasSize(3);
        assertThat(body.get("matches")).hasSize(1);

        JsonNode matchNode = body.get("matches").get(0);
        assertThat(matchNode.get("homeScore").asInt()).isEqualTo(2);
        assertThat(matchNode.get("awayScore").asInt()).isEqualTo(1);
        assertThat(matchNode.get("status").asText()).isEqualTo("FINISHED");
        assertThat(matchNode.get("predictions")).hasSize(3);

        JsonNode viewerPrediction = predictionFor(matchNode, viewer.getUsername());
        assertThat(viewerPrediction.get("predictedHomeScore").asInt()).isEqualTo(2);
        assertThat(viewerPrediction.get("predictedAwayScore").asInt()).isEqualTo(1);
        assertThat(viewerPrediction.get("points").asInt()).isEqualTo(3);
        assertThat(viewerPrediction.get("outcome").asText()).isEqualTo("EXACT_SCORE");

        JsonNode alicePrediction = predictionFor(matchNode, alice.getUsername());
        assertThat(alicePrediction.get("points").asInt()).isEqualTo(1);
        assertThat(alicePrediction.get("outcome").asText()).isEqualTo("CORRECT_OUTCOME");

        JsonNode bobPrediction = predictionFor(matchNode, bob.getUsername());
        assertThat(bobPrediction.get("points").asInt()).isZero();
        assertThat(bobPrediction.get("outcome").asText()).isEqualTo("INCORRECT");
    }

    @Test
    void incompleteGameweekIsHiddenAndReturnsConflict() throws Exception {
        Match match = createMatch("gw-incomplete", INCOMPLETE_KICKOFF, MatchStatus.SCHEDULED, null, null);
        createPrediction(alice, match, 1, 0, 0);

        String token = loginAndGetToken(viewer.getUsername());

        ResponseEntity<String> listResponse = get(token, gameweeksPath());
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(listResponse.getBody())).isEmpty();

        ResponseEntity<String> detailResponse = get(
                token, gameweekPath(INCOMPLETE_KICKOFF));

        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode body = objectMapper.readTree(detailResponse.getBody());
        assertThat(body.get("error").asText()).isEqualTo("GAMEWEEK_NOT_COMPLETED");
    }

    @Test
    void finishedGameweekWithoutResultsIsHidden() throws Exception {
        Match match = createMatch("gw-no-result", NO_RESULT_KICKOFF, MatchStatus.FINISHED, null, null);
        createPrediction(alice, match, 1, 0, 0);

        String token = loginAndGetToken(viewer.getUsername());

        ResponseEntity<String> listResponse = get(token, gameweeksPath());
        assertThat(objectMapper.readTree(listResponse.getBody())).isEmpty();

        ResponseEntity<String> detailResponse = get(
                token, gameweekPath(NO_RESULT_KICKOFF));
        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void gameweekWithUnscoredPredictionsIsHidden() throws Exception {
        Match match = createMatch("gw-unscored", UNSCORED_KICKOFF, MatchStatus.FINISHED, 2, 1);
        createPrediction(alice, match, 2, 1, 0);

        String token = loginAndGetToken(viewer.getUsername());

        ResponseEntity<String> listResponse = get(token, gameweeksPath());
        assertThat(objectMapper.readTree(listResponse.getBody())).isEmpty();

        ResponseEntity<String> detailResponse = get(
                token, gameweekPath(UNSCORED_KICKOFF));
        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void onlyCompletedGameweeksAreListed() throws Exception {
        Match completed = createMatch(
                "gw-list-complete", COMPLETED_KICKOFF, MatchStatus.FINISHED, 1, 0);
        createPrediction(alice, completed, 1, 0, 3);

        createMatch("gw-list-incomplete", INCOMPLETE_KICKOFF, MatchStatus.SCHEDULED, null, null);

        String token = loginAndGetToken(viewer.getUsername());

        ResponseEntity<String> response = get(token, gameweeksPath());

        JsonNode gameweeks = objectMapper.readTree(response.getBody());
        assertThat(gameweeks).hasSize(1);
        assertThat(gameweeks.get(0).get("gameweek").asInt())
                .isEqualTo(isoWeek(COMPLETED_KICKOFF));
    }

    @Test
    void nonMemberCannotAccessLeagueComparisons() throws Exception {
        Match match = createMatch("gw-non-member", COMPLETED_KICKOFF, MatchStatus.FINISHED, 1, 0);
        createPrediction(alice, match, 1, 0, 3);

        String token = loginAndGetToken(outsider.getUsername());

        ResponseEntity<String> listResponse = get(token, gameweeksPath());
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> detailResponse = get(
                token, gameweekPath(COMPLETED_KICKOFF));
        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void memberIdFilterRevealsOnlyRequestedMember() throws Exception {
        Match match = createMatch("gw-member-filter", COMPLETED_KICKOFF, MatchStatus.FINISHED, 2, 1);
        createPrediction(viewer, match, 2, 1, 3);
        createPrediction(alice, match, 3, 0, 1);
        createPrediction(bob, match, 0, 2, 0);

        String token = loginAndGetToken(viewer.getUsername());

        ResponseEntity<String> response = get(
                token,
                gameweekPath(COMPLETED_KICKOFF) + "?memberId=" + alice.getId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("members")).hasSize(1);
        assertThat(body.get("members").get(0).get("userId").asText())
                .isEqualTo(alice.getId().toString());
        assertThat(body.get("matches").get(0).get("predictions")).hasSize(1);
        assertThat(body.get("matches").get(0).get("predictions").get(0).get("username").asText())
                .isEqualTo(alice.getUsername());
    }

    @Test
    void memberIdThatIsNotInLeagueIsRejected() throws Exception {
        Match match = createMatch("gw-bad-member", COMPLETED_KICKOFF, MatchStatus.FINISHED, 2, 1);
        createPrediction(alice, match, 3, 0, 1);

        String token = loginAndGetToken(viewer.getUsername());

        ResponseEntity<String> response = get(
                token,
                gameweekPath(COMPLETED_KICKOFF) + "?memberId=" + outsider.getId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unrelatedUsersPredictionsAreNotExposed() throws Exception {
        Match match = createMatch("gw-unrelated", COMPLETED_KICKOFF, MatchStatus.FINISHED, 2, 1);
        createPrediction(alice, match, 3, 0, 1);
        createPrediction(outsider, match, 9, 9, 0);

        String token = loginAndGetToken(viewer.getUsername());

        ResponseEntity<String> response = get(
                token, gameweekPath(COMPLETED_KICKOFF));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).doesNotContain(outsider.getUsername());
        assertThat(response.getBody()).doesNotContain("@example.com");
        assertThat(response.getBody()).contains(alice.getUsername());
    }

    @Test
    void gameweekUnlocksAfterResultAndRecalculation() throws Exception {
        Match match = createMatch(
                "gw-unlock", UNLOCK_KICKOFF, MatchStatus.SCHEDULED, null, null);
        createPrediction(alice, match, 2, 1, 0);

        String token = loginAndGetToken(viewer.getUsername());

        ResponseEntity<String> hiddenResponse = get(
                token, gameweekPath(UNLOCK_KICKOFF));
        assertThat(hiddenResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        match.setStatus(MatchStatus.FINISHED);
        match.setHomeScore(2);
        match.setAwayScore(1);
        matchRepository.save(match);
        predictionRecalculationService.recalculate(match, null);

        ResponseEntity<String> unlockedResponse = get(
                token, gameweekPath(UNLOCK_KICKOFF));

        assertThat(unlockedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(unlockedResponse.getBody());
        JsonNode prediction = predictionFor(body.get("matches").get(0), alice.getUsername());
        assertThat(prediction.get("points").asInt()).isEqualTo(3);
        assertThat(prediction.get("outcome").asText()).isEqualTo("EXACT_SCORE");
    }

    @Test
    void unauthenticatedUserCannotAccessComparisons() {
        ResponseEntity<String> response = get(null, gameweeksPath());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void unknownLeagueReturnsNotFound() throws Exception {
        String token = loginAndGetToken(viewer.getUsername());

        ResponseEntity<String> response = get(
                token,
                "/api/leagues/" + UUID.randomUUID() + "/comparisons/gameweeks"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void multipleCompletedGameweeksAreListedNewestFirst() throws Exception {
        Instant earlier = Instant.parse("2026-04-06T18:00:00Z");
        Instant later = Instant.parse("2026-04-20T18:00:00Z");

        Match earlierMatch = createMatch("gw-multi-earlier", earlier, MatchStatus.FINISHED, 1, 0);
        createPrediction(alice, earlierMatch, 1, 0, 3);

        Match laterMatch = createMatch("gw-multi-later", later, MatchStatus.FINISHED, 2, 2);
        createPrediction(alice, laterMatch, 2, 2, 3);

        String token = loginAndGetToken(viewer.getUsername());

        ResponseEntity<String> response = get(token, gameweeksPath());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode gameweeks = objectMapper.readTree(response.getBody());
        assertThat(gameweeks).hasSize(2);
        assertThat(gameweeks.get(0).get("gameweek").asInt()).isEqualTo(isoWeek(later));
        assertThat(gameweeks.get(0).get("season").asInt()).isEqualTo(season(later));
        assertThat(gameweeks.get(1).get("gameweek").asInt()).isEqualTo(isoWeek(earlier));

        assertThat(get(token, gameweekPath(later)).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get(token, gameweekPath(earlier)).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void memberWithoutPredictionIsRevealedAsNoPrediction() throws Exception {
        Match match = createMatch("gw-missing-member", COMPLETED_KICKOFF, MatchStatus.FINISHED, 2, 1);
        createPrediction(viewer, match, 2, 1, 3);

        String token = loginAndGetToken(viewer.getUsername());

        ResponseEntity<String> response = get(token, gameweekPath(COMPLETED_KICKOFF));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("members")).hasSize(3);

        JsonNode alicePrediction = predictionFor(body.get("matches").get(0), alice.getUsername());
        assertThat(alicePrediction.get("outcome").asText()).isEqualTo("NO_PREDICTION");
        assertThat(alicePrediction.get("predictedHomeScore").isNull()).isTrue();
        assertThat(alicePrediction.get("predictedAwayScore").isNull()).isTrue();
        assertThat(alicePrediction.get("points").isNull()).isTrue();
    }

    private String gameweeksPath() {
        return "/api/leagues/" + league.getId() + "/comparisons/gameweeks";
    }

    private String gameweekPath(Instant kickoff) {
        return gameweeksPath() + "/" + season(kickoff) + "/" + isoWeek(kickoff);
    }

    private int season(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).getYear();
    }

    private int isoWeek(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    private JsonNode predictionFor(JsonNode matchNode, String username) {
        for (JsonNode prediction : matchNode.get("predictions")) {
            if (prediction.get("username").asText().equals(username)) {
                return prediction;
            }
        }

        throw new AssertionError("No prediction found for user " + username);
    }

    private ResponseEntity<String> get(String token, String path) {
        HttpHeaders headers = new HttpHeaders();

        if (token != null) {
            headers.setBearerAuth(token);
        }

        return restTemplate.exchange(
                path,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
    }

    private User createUser(String username) {
        return userRepository.save(
                new User(
                        username,
                        username + "@example.com",
                        passwordEncoder.encode("password123")
                )
        );
    }

    private Match createMatch(
            String externalId,
            Instant kickoffAt,
            MatchStatus status,
            Integer homeScore,
            Integer awayScore
    ) {
        Match match = new Match(externalId, "Home FC", "Away FC", kickoffAt, status);
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);

        return matchRepository.save(match);
    }

    private Prediction createPrediction(
            User user,
            Match match,
            int predictedHomeScore,
            int predictedAwayScore,
            int points
    ) {
        Prediction prediction = new Prediction(
                user,
                match,
                predictedHomeScore,
                predictedAwayScore
        );
        prediction.setPoints(points);

        return predictionRepository.save(prediction);
    }

    private String randomJoinCode() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8);
    }

    private String loginAndGetToken(String username) throws Exception {
        String requestBody = """
                {
                    "username": "%s",
                    "password": "password123"
                }
                """.formatted(username);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(requestBody, headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        return objectMapper.readTree(response.getBody()).get("accessToken").asText();
    }
}
