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

/**
 * Integration coverage for the league prediction comparison.
 *
 * <p>Gameweeks are the provider matchday scoped to a competition and season —
 * not the ISO week of the kickoff date. Matchday 1 of the configured
 * competition kicks off in ISO week 37, which is exactly the regression this
 * suite guards against.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LeagueComparisonControllerIntegrationTest {

    private static final String COMPETITION = "CL";
    private static final int SEASON = 2026;

    // 2026-09-08 is ISO week 37, the unrelated label the old code showed.
    private static final Instant MATCHDAY_ONE_KICKOFF = Instant.parse("2026-09-08T18:00:00Z");
    private static final Instant MATCHDAY_TWO_KICKOFF = Instant.parse("2026-10-13T18:00:00Z");
    private static final Instant MATCHDAY_THREE_KICKOFF = Instant.parse("2026-10-20T18:00:00Z");
    private static final Instant MATCHDAY_FOUR_KICKOFF = Instant.parse("2026-11-03T18:00:00Z");

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
    void gameweekOneIsReportedInsteadOfTheKickoffIsoWeek() throws Exception {
        Match match = createMatch("gw-one", 1, MATCHDAY_ONE_KICKOFF, MatchStatus.FINISHED, 2, 1);
        createScoredPrediction(viewer, match, 2, 1);
        createScoredPrediction(alice, match, 3, 0);
        createScoredPrediction(bob, match, 0, 2);

        String token = loginAndGetToken(viewer.getUsername());

        ResponseEntity<String> listResponse = get(token, gameweeksPath());
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode gameweeks = objectMapper.readTree(listResponse.getBody());
        assertThat(gameweeks).hasSize(1);
        assertThat(gameweeks.get(0).get("competition").asText()).isEqualTo(COMPETITION);
        assertThat(gameweeks.get(0).get("season").asInt()).isEqualTo(SEASON);
        assertThat(gameweeks.get(0).get("gameweek").asInt()).isEqualTo(1);
        assertThat(gameweeks.get(0).get("label").asText()).isEqualTo("Gameweek 1");
        assertThat(gameweeks.get(0).get("matchCount").asInt()).isEqualTo(1);
        assertThat(gameweeks.get(0).get("eligibleMatchCount").asInt()).isEqualTo(1);

        // Sanity-check the regression: ISO week of that kickoff really is 37.
        assertThat(MATCHDAY_ONE_KICKOFF.atZone(ZoneOffset.UTC)
                .get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)).isEqualTo(37);

        ResponseEntity<String> detailResponse = get(token, gameweekPath(1));
        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode body = objectMapper.readTree(detailResponse.getBody());
        assertThat(body.get("leagueId").asText()).isEqualTo(league.getId().toString());
        assertThat(body.get("competition").asText()).isEqualTo(COMPETITION);
        assertThat(body.get("season").asInt()).isEqualTo(SEASON);
        assertThat(body.get("gameweek").asInt()).isEqualTo(1);
        assertThat(body.get("members")).hasSize(3);
        assertThat(body.get("matches")).hasSize(1);

        JsonNode matchNode = body.get("matches").get(0);
        assertThat(matchNode.get("homeScore").asInt()).isEqualTo(2);
        assertThat(matchNode.get("awayScore").asInt()).isEqualTo(1);
        assertThat(matchNode.get("status").asText()).isEqualTo("FINISHED");

        JsonNode viewerPrediction = predictionFor(matchNode, viewer.getUsername());
        assertThat(viewerPrediction.get("predictedHomeScore").asInt()).isEqualTo(2);
        assertThat(viewerPrediction.get("predictedAwayScore").asInt()).isEqualTo(1);
        assertThat(viewerPrediction.get("points").asInt()).isEqualTo(3);
        assertThat(viewerPrediction.get("outcome").asText()).isEqualTo("EXACT_SCORE");

        assertThat(predictionFor(matchNode, alice.getUsername()).get("points").asInt()).isEqualTo(1);
        assertThat(predictionFor(matchNode, bob.getUsername()).get("points").asInt()).isZero();
    }

    @Test
    void multipleGameweeksAreListedNewestFirstAndEachIsSelectable() throws Exception {
        Match first = createMatch("multi-1", 1, MATCHDAY_ONE_KICKOFF, MatchStatus.FINISHED, 1, 0);
        createScoredPrediction(alice, first, 1, 0);
        Match second = createMatch("multi-2", 2, MATCHDAY_TWO_KICKOFF, MatchStatus.FINISHED, 2, 2);
        createScoredPrediction(alice, second, 2, 2);

        String token = loginAndGetToken(viewer.getUsername());

        JsonNode gameweeks = objectMapper.readTree(get(token, gameweeksPath()).getBody());
        assertThat(gameweeks).hasSize(2);
        assertThat(gameweeks.get(0).get("gameweek").asInt()).isEqualTo(2);
        assertThat(gameweeks.get(1).get("gameweek").asInt()).isEqualTo(1);

        assertThat(get(token, gameweekPath(1)).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get(token, gameweekPath(2)).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void fullyCompletedGameweekShowsEveryMatch() throws Exception {
        Match first = createMatch("full-1", 2, MATCHDAY_TWO_KICKOFF, MatchStatus.FINISHED, 1, 1);
        createScoredPrediction(alice, first, 1, 1);
        Match second = createMatch("full-2", 2, MATCHDAY_TWO_KICKOFF.plusSeconds(3600),
                MatchStatus.FINISHED, 3, 0);
        createScoredPrediction(alice, second, 2, 0);

        String token = loginAndGetToken(viewer.getUsername());

        JsonNode body = objectMapper.readTree(get(token, gameweekPath(2)).getBody());
        assertThat(body.get("matches")).hasSize(2);
    }

    @Test
    void partiallyCompletedGameweekOnlyShowsScoredMatches() throws Exception {
        Match finished = createMatch("partial-finished", 3, MATCHDAY_THREE_KICKOFF,
                MatchStatus.FINISHED, 2, 1);
        createScoredPrediction(alice, finished, 2, 1);
        createMatch("partial-upcoming", 3, MATCHDAY_THREE_KICKOFF.plusSeconds(3600),
                MatchStatus.SCHEDULED, null, null);

        String token = loginAndGetToken(viewer.getUsername());

        JsonNode summary = objectMapper.readTree(get(token, gameweeksPath()).getBody()).get(0);
        assertThat(summary.get("gameweek").asInt()).isEqualTo(3);
        assertThat(summary.get("matchCount").asInt()).isEqualTo(2);
        assertThat(summary.get("eligibleMatchCount").asInt()).isEqualTo(1);

        JsonNode body = objectMapper.readTree(get(token, gameweekPath(3)).getBody());
        assertThat(body.get("matches")).hasSize(1);
        assertThat(body.get("matches").get(0).get("homeScore").asInt()).isEqualTo(2);
        assertThat(body.get("matches").get(0).get("awayScore").asInt()).isEqualTo(1);
    }

    @Test
    void finishedMatchWhoseScoringHasNotCompletedStaysHiddenThenAppears() throws Exception {
        Match match = createMatch("unscored", 4, MATCHDAY_FOUR_KICKOFF,
                MatchStatus.FINISHED, 2, 1);
        createPrediction(alice, match, 2, 1, 0);

        String token = loginAndGetToken(viewer.getUsername());

        JsonNode summary = objectMapper.readTree(get(token, gameweeksPath()).getBody()).get(0);
        assertThat(summary.get("matchCount").asInt()).isEqualTo(1);
        assertThat(summary.get("eligibleMatchCount").asInt()).isZero();

        JsonNode hidden = objectMapper.readTree(get(token, gameweekPath(4)).getBody());
        assertThat(hidden.get("matches")).isEmpty();

        predictionRecalculationService.recalculate(match, null);

        JsonNode revealed = objectMapper.readTree(get(token, gameweekPath(4)).getBody());
        assertThat(revealed.get("matches")).hasSize(1);
        assertThat(predictionFor(revealed.get("matches").get(0), alice.getUsername())
                .get("points").asInt()).isEqualTo(3);
    }

    @Test
    void unfinishedMatchesAreNeverShown() throws Exception {
        Match scheduled = createMatch("scheduled", 5, MATCHDAY_FOUR_KICKOFF,
                MatchStatus.SCHEDULED, null, null);
        createPrediction(alice, scheduled, 1, 0, 0);

        Match live = createMatch("live", 5, MATCHDAY_FOUR_KICKOFF.plusSeconds(3600),
                MatchStatus.LIVE, 1, 0);

        String token = loginAndGetToken(viewer.getUsername());

        JsonNode body = objectMapper.readTree(get(token, gameweekPath(5)).getBody());
        assertThat(body.get("matches")).isEmpty();
    }

    @Test
    void matchBecomesVisibleAfterResultSynchronizationAndScoring() throws Exception {
        Match match = createMatch("unlock", 6, MATCHDAY_FOUR_KICKOFF,
                MatchStatus.SCHEDULED, null, null);
        createPrediction(alice, match, 2, 0, 0);

        String token = loginAndGetToken(viewer.getUsername());

        assertThat(objectMapper.readTree(get(token, gameweekPath(6)).getBody())
                .get("matches")).isEmpty();

        match.setStatus(MatchStatus.FINISHED);
        match.setHomeScore(2);
        match.setAwayScore(0);
        matchRepository.save(match);
        predictionRecalculationService.recalculate(match, null);

        JsonNode body = objectMapper.readTree(get(token, gameweekPath(6)).getBody());
        assertThat(body.get("matches")).hasSize(1);
        JsonNode prediction = predictionFor(body.get("matches").get(0), alice.getUsername());
        assertThat(prediction.get("points").asInt()).isEqualTo(3);
        assertThat(prediction.get("outcome").asText()).isEqualTo("EXACT_SCORE");
    }

    @Test
    void memberWithoutPredictionIsRevealedAsNoPrediction() throws Exception {
        Match match = createMatch("missing-member", 7, MATCHDAY_FOUR_KICKOFF,
                MatchStatus.FINISHED, 2, 1);
        createScoredPrediction(viewer, match, 2, 1);

        String token = loginAndGetToken(viewer.getUsername());

        ResponseEntity<String> response = get(token, gameweekPath(7));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("members")).hasSize(3);

        JsonNode alicePrediction = predictionFor(body.get("matches").get(0), alice.getUsername());
        assertThat(alicePrediction.get("outcome").asText()).isEqualTo("NO_PREDICTION");
        assertThat(alicePrediction.get("predictedHomeScore").isNull()).isTrue();
        assertThat(alicePrediction.get("predictedAwayScore").isNull()).isTrue();
        assertThat(alicePrediction.get("points").isNull()).isTrue();
    }

    @Test
    void memberIdFilterRevealsOnlyRequestedMember() throws Exception {
        Match match = createMatch("member-filter", 8, MATCHDAY_FOUR_KICKOFF,
                MatchStatus.FINISHED, 2, 1);
        createScoredPrediction(viewer, match, 2, 1);
        createScoredPrediction(alice, match, 3, 0);
        createScoredPrediction(bob, match, 0, 2);

        String token = loginAndGetToken(viewer.getUsername());

        ResponseEntity<String> response = get(
                token,
                gameweekPath(8) + "?memberId=" + alice.getId()
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
        Match match = createMatch("bad-member", 9, MATCHDAY_FOUR_KICKOFF,
                MatchStatus.FINISHED, 2, 1);
        createScoredPrediction(alice, match, 3, 0);

        String token = loginAndGetToken(viewer.getUsername());

        ResponseEntity<String> response = get(
                token,
                gameweekPath(9) + "?memberId=" + outsider.getId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unrelatedUsersPredictionsAreNotExposed() throws Exception {
        Match match = createMatch("unrelated", 10, MATCHDAY_FOUR_KICKOFF,
                MatchStatus.FINISHED, 2, 1);
        createScoredPrediction(alice, match, 3, 0);
        createPrediction(outsider, match, 9, 9, 0);

        String token = loginAndGetToken(viewer.getUsername());

        ResponseEntity<String> response = get(token, gameweekPath(10));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).doesNotContain(outsider.getUsername());
        assertThat(response.getBody()).doesNotContain("@example.com");
        assertThat(response.getBody()).contains(alice.getUsername());
    }

    @Test
    void nonMemberCannotAccessLeagueComparisons() throws Exception {
        Match match = createMatch("non-member", 11, MATCHDAY_FOUR_KICKOFF,
                MatchStatus.FINISHED, 1, 0);
        createScoredPrediction(alice, match, 1, 0);

        String token = loginAndGetToken(outsider.getUsername());

        assertThat(get(token, gameweeksPath()).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get(token, gameweekPath(11)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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
    void unknownGameweekReturnsNotFound() throws Exception {
        Match match = createMatch("known", 1, MATCHDAY_ONE_KICKOFF, MatchStatus.FINISHED, 1, 0);
        createScoredPrediction(alice, match, 1, 0);

        String token = loginAndGetToken(viewer.getUsername());

        assertThat(get(token, gameweekPath(99)).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void emptyGameweekWithoutEligibleMatchesIsReturnedEmpty() throws Exception {
        createMatch("empty-gw", 12, MATCHDAY_FOUR_KICKOFF, MatchStatus.SCHEDULED, null, null);

        String token = loginAndGetToken(viewer.getUsername());

        JsonNode summary = objectMapper.readTree(get(token, gameweeksPath()).getBody()).get(0);
        assertThat(summary.get("gameweek").asInt()).isEqualTo(12);
        assertThat(summary.get("eligibleMatchCount").asInt()).isZero();

        ResponseEntity<String> response = get(token, gameweekPath(12));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("matches")).isEmpty();
    }

    private String gameweeksPath() {
        return "/api/leagues/" + league.getId() + "/comparisons/gameweeks";
    }

    private String gameweekPath(int matchday) {
        return gameweeksPath() + "/" + COMPETITION + "/" + SEASON + "/" + matchday;
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
            int matchday,
            Instant kickoffAt,
            MatchStatus status,
            Integer homeScore,
            Integer awayScore
    ) {
        Match match = new Match(
                externalId,
                "Home FC",
                "Away FC",
                kickoffAt,
                status,
                COMPETITION,
                SEASON,
                matchday
        );
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

    private void createScoredPrediction(
            User user,
            Match match,
            int predictedHomeScore,
            int predictedAwayScore
    ) {
        createPrediction(user, match, predictedHomeScore, predictedAwayScore, 0);
        predictionRecalculationService.recalculate(match, null);
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
