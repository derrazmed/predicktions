package com.ven.predicktions.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.MatchStatus;
import com.ven.predicktions.model.Prediction;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.MatchRepository;
import com.ven.predicktions.repository.PredictionRepository;
import com.ven.predicktions.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PredictionControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User user;
    private Match match;

    @BeforeEach
    void setUp() {
        predictionRepository.deleteAll();
        matchRepository.deleteAll();
        userRepository.deleteAll();

        user = createUser("user-" + UUID.randomUUID());

        match = createMatch(
                "match-" + UUID.randomUUID(),
                Instant.now().plusSeconds(3600),
                MatchStatus.SCHEDULED
        );
    }

    @Test
    void authenticatedUserCanCreatePrediction() throws Exception {
        String token = loginAndGetToken(
                user.getUsername(),
                "password123"
        );

        String requestBody = """
                {
                    "matchId": "%s",
                    "predictedHomeScore": 2,
                    "predictedAwayScore": 1
                }
                """.formatted(match.getId());

        ResponseEntity<String> response = postPrediction(
                token,
                requestBody
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(body.get("matchId").asText())
                .isEqualTo(match.getId().toString());

        assertThat(body.get("predictedHomeScore").asInt())
                .isEqualTo(2);

        assertThat(body.get("predictedAwayScore").asInt())
                .isEqualTo(1);

        assertThat(body.get("points").asInt())
                .isEqualTo(0);

        assertThat(predictionRepository
                .findByUserIdAndMatchId(user.getId(), match.getId()))
                .isPresent();
    }

    @Test
    void authenticatedUserCanRetrievePredictions() throws Exception {
        Prediction prediction = new Prediction(
                user,
                match,
                2,
                1
        );

        predictionRepository.save(prediction);

        String token = loginAndGetToken(
                user.getUsername(),
                "password123"
        );

        ResponseEntity<String> response = getPredictions(token);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(body.isArray()).isTrue();
        assertThat(body).hasSize(1);

        JsonNode returnedPrediction = body.get(0);

        assertThat(returnedPrediction.get("id").asText())
                .isEqualTo(prediction.getId().toString());

        assertThat(returnedPrediction.get("matchId").asText())
                .isEqualTo(match.getId().toString());

        assertThat(returnedPrediction.get("predictedHomeScore").asInt())
                .isEqualTo(2);

        assertThat(returnedPrediction.get("predictedAwayScore").asInt())
                .isEqualTo(1);

        assertThat(returnedPrediction.get("points").asInt())
                .isEqualTo(0);
    }

    @Test
    void unauthenticatedUserCannotCreatePrediction() {
        String requestBody = """
                {
                    "matchId": "%s",
                    "predictedHomeScore": 2,
                    "predictedAwayScore": 1
                }
                """.formatted(match.getId());

        ResponseEntity<String> response = postPrediction(
                null,
                requestBody
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat(predictionRepository.count())
                .isZero();
    }

    @Test
    void unauthenticatedUserCannotRetrievePredictions() {
        ResponseEntity<String> response = getPredictions(null);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void nonExistentMatchIsRejected() throws Exception {
        String token = loginAndGetToken(
                user.getUsername(),
                "password123"
        );

        String requestBody = """
                {
                    "matchId": "%s",
                    "predictedHomeScore": 2,
                    "predictedAwayScore": 1
                }
                """.formatted(UUID.randomUUID());

        ResponseEntity<String> response = postPrediction(
                token,
                requestBody
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void negativePredictedScoreIsRejected() throws Exception {
        String token = loginAndGetToken(
                user.getUsername(),
                "password123"
        );

        String requestBody = """
                {
                    "matchId": "%s",
                    "predictedHomeScore": -1,
                    "predictedAwayScore": 1
                }
                """.formatted(match.getId());

        ResponseEntity<String> response = postPrediction(
                token,
                requestBody
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(predictionRepository.count())
                .isZero();
    }

    @Test
    void duplicatePredictionIsRejected() throws Exception {
        Prediction existingPrediction = new Prediction(
                user,
                match,
                2,
                1
        );

        predictionRepository.save(existingPrediction);

        String token = loginAndGetToken(
                user.getUsername(),
                "password123"
        );

        String requestBody = """
                {
                    "matchId": "%s",
                    "predictedHomeScore": 3,
                    "predictedAwayScore": 2
                }
                """.formatted(match.getId());

        ResponseEntity<String> response = postPrediction(
                token,
                requestBody
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(predictionRepository
                .findAllByUserId(user.getId()))
                .hasSize(1);
    }

    @Test
    void predictionBeforeKickoffSucceeds() throws Exception {
        Match upcomingMatch = createMatch(
                "upcoming-" + UUID.randomUUID(),
                Instant.now().plusSeconds(3600),
                MatchStatus.SCHEDULED
        );

        String token = loginAndGetToken(
                user.getUsername(),
                "password123"
        );

        String requestBody = """
                {
                    "matchId": "%s",
                    "predictedHomeScore": 2,
                    "predictedAwayScore": 1
                }
                """.formatted(upcomingMatch.getId());

        ResponseEntity<String> response = postPrediction(
                token,
                requestBody
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void predictionAtKickoffIsRejected() throws Exception {
        Match kickoffMatch = createMatch(
                "kickoff-" + UUID.randomUUID(),
                Instant.now(),
                MatchStatus.SCHEDULED
        );

        String token = loginAndGetToken(
                user.getUsername(),
                "password123"
        );

        String requestBody = """
                {
                    "matchId": "%s",
                    "predictedHomeScore": 2,
                    "predictedAwayScore": 1
                }
                """.formatted(kickoffMatch.getId());

        ResponseEntity<String> response = postPrediction(
                token,
                requestBody
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void predictionAfterKickoffIsRejected() throws Exception {
        Match startedMatch = createMatch(
                "started-" + UUID.randomUUID(),
                Instant.now().minusSeconds(3600),
                MatchStatus.SCHEDULED
        );

        String token = loginAndGetToken(
                user.getUsername(),
                "password123"
        );

        String requestBody = """
                {
                    "matchId": "%s",
                    "predictedHomeScore": 2,
                    "predictedAwayScore": 1
                }
                """.formatted(startedMatch.getId());

        ResponseEntity<String> response = postPrediction(
                token,
                requestBody
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void userCannotAccessAnotherUsersPredictions() throws Exception {
        User otherUser = createUser(
                "other-" + UUID.randomUUID()
        );

        Match otherMatch = createMatch(
                "other-match-" + UUID.randomUUID(),
                Instant.now().plusSeconds(3600),
                MatchStatus.SCHEDULED
        );

        Prediction userPrediction = new Prediction(
                user,
                match,
                2,
                1
        );

        Prediction otherPrediction = new Prediction(
                otherUser,
                otherMatch,
                3,
                0
        );

        predictionRepository.save(userPrediction);
        predictionRepository.save(otherPrediction);

        String token = loginAndGetToken(
                user.getUsername(),
                "password123"
        );

        ResponseEntity<String> response = getPredictions(token);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(body).hasSize(1);

        assertThat(body.get(0).get("id").asText())
                .isEqualTo(userPrediction.getId().toString());

        assertThat(body.get(0).get("id").asText())
                .isNotEqualTo(otherPrediction.getId().toString());
    }

    @Test
    void predictionDataIsPersistedCorrectly() throws Exception {
        String token = loginAndGetToken(
                user.getUsername(),
                "password123"
        );

        String requestBody = """
                {
                    "matchId": "%s",
                    "predictedHomeScore": 4,
                    "predictedAwayScore": 2
                }
                """.formatted(match.getId());

        ResponseEntity<String> response = postPrediction(
                token,
                requestBody
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        Prediction savedPrediction = predictionRepository
                .findByUserIdAndMatchId(user.getId(), match.getId())
                .orElseThrow();

        assertThat(savedPrediction.getPredictedHomeScore())
                .isEqualTo(4);

        assertThat(savedPrediction.getPredictedAwayScore())
                .isEqualTo(2);

        assertThat(savedPrediction.getPoints())
                .isEqualTo(0);

        assertThat(savedPrediction.getUser().getId())
                .isEqualTo(user.getId());

        assertThat(savedPrediction.getMatch().getId())
                .isEqualTo(match.getId());

        assertThat(savedPrediction.getCreatedAt())
                .isNotNull();

        assertThat(savedPrediction.getUpdatedAt())
                .isNotNull();
    }

    private ResponseEntity<String> postPrediction(
            String token,
            String requestBody
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (token != null) {
            headers.setBearerAuth(token);
        }

        HttpEntity<String> request = new HttpEntity<>(
                requestBody,
                headers
        );

        return restTemplate.exchange(
                "/api/predictions",
                HttpMethod.POST,
                request,
                String.class
        );
    }

    private ResponseEntity<String> getPredictions(
            String token
    ) {
        HttpHeaders headers = new HttpHeaders();

        if (token != null) {
            headers.setBearerAuth(token);
        }

        HttpEntity<Void> request = new HttpEntity<>(headers);

        return restTemplate.exchange(
                "/api/predictions",
                HttpMethod.GET,
                request,
                String.class
        );
    }

    private User createUser(String username) {
        User user = new User(
                username,
                username + "@example.com",
                passwordEncoder.encode("password123")
        );

        return userRepository.save(user);
    }

    private Match createMatch(
            String externalId,
            Instant kickoffAt,
            MatchStatus status
    ) {
        Match match = new Match(
                externalId,
                "Home FC",
                "Away FC",
                kickoffAt,
                status
        );

        return matchRepository.save(match);
    }

    private String loginAndGetToken(
            String username,
            String password
    ) throws Exception {

        String requestBody = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(
                requestBody,
                headers
        );

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                request,
                String.class
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        JsonNode body = objectMapper.readTree(response.getBody());

        return body.get("accessToken").asText();
    }
}