package com.ven.predicktions.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.LeagueMemberRepository;
import com.ven.predicktions.repository.LeagueRepository;
import com.ven.predicktions.repository.UserRepository;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LeagueControllerIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User user;

    @BeforeEach
    void setUp() {
        leagueMemberRepository.deleteAll();
        leagueRepository.deleteAll();
        userRepository.deleteAll();

        user = createUser("user-" + UUID.randomUUID());
    }

    @Test
    void authenticatedUserCanCreateLeague() throws Exception {
        String token = loginAndGetToken(user.getUsername(), "password123");

        ResponseEntity<String> response = postLeague(
                token,
                """
                        {
                            "name": "Office League"
                        }
                        """
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(body.get("id").asText()).isNotBlank();
        assertThat(body.get("name").asText()).isEqualTo("Office League");
        assertThat(body.get("ownerId").asText()).isEqualTo(user.getId().toString());
        assertThat(body.get("joinCode").asText()).isNotBlank();
        assertThat(body.get("joinCode").asText()).hasSize(8);
        assertThat(body.get("createdAt").asText()).isNotBlank();

        UUID leagueId = UUID.fromString(body.get("id").asText());
        String joinCode = body.get("joinCode").asText();

        assertThat(leagueRepository.findByJoinCode(joinCode)).isPresent();
        assertThat(leagueMemberRepository.findByLeagueIdAndUserId(leagueId, user.getId()))
                .isPresent();
    }

    @Test
    void unauthenticatedUserCannotCreateLeague() {
        ResponseEntity<String> response = postLeague(
                null,
                """
                        {
                            "name": "Office League"
                        }
                        """
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(leagueRepository.count()).isZero();
        assertThat(leagueMemberRepository.count()).isZero();
    }

    @Test
    void blankLeagueNameIsRejected() throws Exception {
        String token = loginAndGetToken(user.getUsername(), "password123");

        ResponseEntity<String> response = postLeague(
                token,
                """
                        {
                            "name": "  "
                        }
                        """
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(leagueRepository.count()).isZero();
        assertThat(leagueMemberRepository.count()).isZero();
    }

    @Test
    void generatedJoinCodesAreUnique() throws Exception {
        String token = loginAndGetToken(user.getUsername(), "password123");

        ResponseEntity<String> firstResponse = postLeague(
                token,
                """
                        {
                            "name": "First League"
                        }
                        """
        );
        ResponseEntity<String> secondResponse = postLeague(
                token,
                """
                        {
                            "name": "Second League"
                        }
                        """
        );

        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String firstJoinCode = objectMapper.readTree(firstResponse.getBody())
                .get("joinCode")
                .asText();
        String secondJoinCode = objectMapper.readTree(secondResponse.getBody())
                .get("joinCode")
                .asText();

        assertThat(firstJoinCode).isNotEqualTo(secondJoinCode);
        assertThat(leagueRepository.count()).isEqualTo(2);
        assertThat(leagueMemberRepository.count()).isEqualTo(2);
    }

    private ResponseEntity<String> postLeague(String token, String requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (token != null) {
            headers.setBearerAuth(token);
        }

        return restTemplate.exchange(
                "/api/leagues",
                HttpMethod.POST,
                new HttpEntity<>(requestBody, headers),
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

    private String loginAndGetToken(String username, String password) throws Exception {
        String requestBody = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);

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
