package com.ven.predicktions.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ven.predicktions.dto.match.UpdateMatchResultRequest;
import com.ven.predicktions.config.SportsSyncProperties;
import com.ven.predicktions.integration.sports.SportsMatch;
import com.ven.predicktions.integration.sports.SportsProvider;
import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.MatchStatus;
import com.ven.predicktions.model.Prediction;
import com.ven.predicktions.model.Role;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.AdminAuditLogRepository;
import com.ven.predicktions.repository.MatchRepository;
import com.ven.predicktions.repository.PredictionRepository;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Verifies that synchronising final results from the sports provider recalculates
 * and persists prediction points, and that the recalculated points are exposed
 * through the predictions API.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MatchSyncScoringIntegrationTest {

    private static final String COMPETITION = "CL";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine");

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        SportsProvider sportsProvider() {
            return mock(SportsProvider.class);
        }
    }

    @Autowired
    private MatchSyncService matchSyncService;

    @Autowired
    private AdminMatchService adminMatchService;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminAuditLogRepository adminAuditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SportsProvider sportsProvider;

    @Autowired
    private SportsSyncProperties sportsSyncProperties;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        predictionRepository.deleteAll();
        matchRepository.deleteAll();
        adminAuditLogRepository.deleteAll();
        userRepository.deleteAll();
        reset(sportsProvider);
    }

    @Test
    void exactScorePredictionGetsThreePointsAfterSynchronisingFinishedMatch() {
        User user = createUser();
        Match match = createMatch("exact", MatchStatus.SCHEDULED, null, null, 1);
        Prediction prediction = createPrediction(user, match, 2, 1);

        stubMatchday(1, finished("exact", 2, 1));

        matchSyncService.synchronizeResults(COMPETITION, 1);

        assertThat(pointsOf(prediction)).isEqualTo(3);
    }

    @Test
    void correctOutcomePredictionGetsOnePointAfterSynchronisingFinishedMatch() {
        User user = createUser();
        Match match = createMatch("outcome", MatchStatus.SCHEDULED, null, null, 1);
        Prediction prediction = createPrediction(user, match, 1, 0);

        stubMatchday(1, finished("outcome", 3, 1));

        matchSyncService.synchronizeResults(COMPETITION, 1);

        assertThat(pointsOf(prediction)).isEqualTo(1);
    }

    @Test
    void incorrectOutcomePredictionGetsZeroPoints() {
        User user = createUser();
        Match match = createMatch("wrong", MatchStatus.SCHEDULED, null, null, 1);
        Prediction prediction = createPrediction(user, match, 0, 2);

        stubMatchday(1, finished("wrong", 2, 1));

        matchSyncService.synchronizeResults(COMPETITION, 1);

        assertThat(pointsOf(prediction)).isZero();
    }

    @Test
    void unfinishedMatchIsNotScoredPrematurely() {
        User user = createUser();
        Match match = createMatch("live", MatchStatus.SCHEDULED, null, null, 1);
        Prediction prediction = createPrediction(user, match, 2, 1);

        stubMatchday(1, new SportsMatch(
                "live", "Home FC", "Away FC", kickoff(),
                null, null, MatchStatus.LIVE));

        matchSyncService.synchronizeResults(COMPETITION, 1);

        assertThat(pointsOf(prediction)).isZero();
    }

    @Test
    void repeatedSynchronisationIsIdempotent() {
        User user = createUser();
        Match match = createMatch("repeat", MatchStatus.SCHEDULED, null, null, 2);
        Prediction prediction = createPrediction(user, match, 2, 1);

        stubMatchday(2, finished("repeat", 2, 1));

        matchSyncService.synchronizeResults(COMPETITION, 2);
        assertThat(pointsOf(prediction)).isEqualTo(3);

        matchSyncService.synchronizeResults(COMPETITION, 2);

        assertThat(pointsOf(prediction)).isEqualTo(3);
        assertThat(predictionRepository.count()).isEqualTo(1);
    }

    @Test
    void laterResultCorrectionRecalculatesPoints() {
        User user = createUser();
        User admin = createUser(Role.ADMIN);
        Match match = createMatch("corrected", MatchStatus.SCHEDULED, null, null, 2);
        Prediction prediction = createPrediction(user, match, 2, 1);

        stubMatchday(2, finished("corrected", 2, 1));
        matchSyncService.synchronizeResults(COMPETITION, 2);
        assertThat(pointsOf(prediction)).isEqualTo(3);

        // Correction through the administrative result-correction flow.
        adminMatchService.updateResult(
                match.getId(),
                new UpdateMatchResultRequest(3, 1),
                admin.getId());

        assertThat(pointsOf(prediction)).isEqualTo(1);
    }

    @Test
    void synchronisingFinishedMatchWithoutPredictionsDoesNotFail() {
        createMatch("no-predictions", MatchStatus.SCHEDULED, null, null, 3);

        stubMatchday(3, finished("no-predictions", 1, 0));

        matchSyncService.synchronizeResults(COMPETITION, 3);

        Match reloaded = matchRepository.findByExternalId("no-predictions").orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(reloaded.getHomeScore()).isEqualTo(1);
        assertThat(reloaded.getAwayScore()).isZero();
    }

    @Test
    void resultsSynchronisationRunsAtNineInTheMorning() {
        assertThat(sportsSyncProperties.getResultsCron()).isEqualTo("0 0 9 * * *");
    }

    @Test
    void updatedPointsAreReturnedByPredictionsApi() throws Exception {
        User user = createUser();
        Match match = createMatch("api", MatchStatus.SCHEDULED, null, null, 4);
        createPrediction(user, match, 2, 1);

        stubMatchday(4, finished("api", 2, 1));
        matchSyncService.synchronizeResults(COMPETITION, 4);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtService.generateToken(user.getId()));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/predictions",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body).hasSize(1);
        assertThat(body.get(0).get("matchId").asText())
                .isEqualTo(match.getId().toString());
        assertThat(body.get(0).get("points").asInt()).isEqualTo(3);
    }

    private void stubMatchday(int matchday, SportsMatch... matches) {
        when(sportsProvider.getMatchdayMatches(COMPETITION, matchday))
                .thenReturn(List.of(matches));
    }

    private SportsMatch finished(String externalId, int homeScore, int awayScore) {
        return new SportsMatch(
                externalId, "Home FC", "Away FC", kickoff(),
                homeScore, awayScore, MatchStatus.FINISHED);
    }

    private Instant kickoff() {
        return Instant.parse("2026-09-10T18:00:00Z");
    }

    private int pointsOf(Prediction prediction) {
        return predictionRepository.findById(prediction.getId())
                .orElseThrow()
                .getPoints();
    }

    private User createUser() {
        return createUser(Role.USER);
    }

    private User createUser(Role role) {
        String username = "user-" + UUID.randomUUID();
        return userRepository.save(new User(
                username,
                username + "@example.com",
                passwordEncoder.encode("password123"),
                role));
    }

    private Match createMatch(
            String externalId,
            MatchStatus status,
            Integer homeScore,
            Integer awayScore,
            int matchday
    ) {
        Match match = new Match(
                externalId, "Home FC", "Away FC", kickoff(),
                status, COMPETITION, 2026, matchday);
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        return matchRepository.save(match);
    }

    private Prediction createPrediction(
            User user,
            Match match,
            int predictedHomeScore,
            int predictedAwayScore
    ) {
        return predictionRepository.save(new Prediction(
                user, match, predictedHomeScore, predictedAwayScore));
    }
}
