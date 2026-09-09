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

        match = createMatch("match-" + UUID.randomUUID());
    }

    @Test
    void authenticatedUserCanRetrieveOwnPredictions() throws Exception {
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

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/predictions",
                HttpMethod.GET,
                request,
                String.class
        );

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
    void userCanOnlyRetrieveOwnPredictions() throws Exception {
        User otherUser = createUser("other-" + UUID.randomUUID());

        Match otherMatch = createMatch("match-" + UUID.randomUUID());

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

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/predictions",
                HttpMethod.GET,
                request,
                String.class
        );

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
    void userWithNoPredictionsReceivesEmptyList() throws Exception {
        String token = loginAndGetToken(
                user.getUsername(),
                "password123"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/predictions",
                HttpMethod.GET,
                request,
                String.class
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(body.isArray()).isTrue();
        assertThat(body).isEmpty();
    }

    @Test
    void unauthenticatedUserCannotRetrievePredictions() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/predictions",
                String.class
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void predictionResponseDoesNotExposePasswordInformation()
            throws Exception {

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

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/predictions",
                HttpMethod.GET,
                request,
                String.class
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        String responseBody = response.getBody();

        assertThat(responseBody).doesNotContain("password");
        assertThat(responseBody).doesNotContain("passwordHash");
        assertThat(responseBody).doesNotContain("password_hash");
    }

    private User createUser(String username) {
        User user = new User(
                username,
                username + "@example.com",
                passwordEncoder.encode("password123")
        );

        return userRepository.save(user);
    }

    private Match createMatch(String externalId) {
        Match match = new Match(
                externalId,
                "Home FC",
                "Away FC",
                Instant.now().plusSeconds(3600),
                MatchStatus.SCHEDULED
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