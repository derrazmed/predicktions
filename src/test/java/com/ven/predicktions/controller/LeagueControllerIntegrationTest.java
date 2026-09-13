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

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

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

    @Test
    void authenticatedUserCanJoinLeagueWithJoinCode() throws Exception {
        User owner = createUser("owner-" + UUID.randomUUID());
        String ownerToken = loginAndGetToken(owner.getUsername(), "password123");
        JsonNode league = objectMapper.readTree(
                postLeague(ownerToken, """
                        {
                            "name": "Office League"
                        }
                        """).getBody()
        );
        UUID leagueId = UUID.fromString(league.get("id").asText());
        String joinCode = league.get("joinCode").asText();

        String memberToken = loginAndGetToken(user.getUsername(), "password123");
        ResponseEntity<String> response = postJoin(memberToken, joinCode);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("id").asText()).isEqualTo(leagueId.toString());
        assertThat(body.get("joinCode").asText()).isEqualTo(joinCode);
        assertThat(leagueMemberRepository.findByLeagueIdAndUserId(leagueId, user.getId()))
                .isPresent();
        assertThat(leagueMemberRepository.findAllByLeagueId(leagueId)).hasSize(2);
    }

    @Test
    void unauthenticatedUserCannotJoinLeague() throws Exception {
        User owner = createUser("owner-" + UUID.randomUUID());
        String ownerToken = loginAndGetToken(owner.getUsername(), "password123");
        String joinCode = objectMapper.readTree(
                postLeague(ownerToken, """
                        {
                            "name": "Office League"
                        }
                        """).getBody()
        ).get("joinCode").asText();

        ResponseEntity<String> response = postJoin(null, joinCode);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(leagueMemberRepository.count()).isEqualTo(1);
    }

    @Test
    void invalidJoinCodeIsRejected() throws Exception {
        String token = loginAndGetToken(user.getUsername(), "password123");

        ResponseEntity<String> response = postJoin(token, "UNKNOWN1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("error").asText()).isEqualTo("NOT_FOUND");
        assertThat(leagueMemberRepository.count()).isZero();
    }

    @Test
    void userCannotJoinTheSameLeagueTwice() throws Exception {
        User owner = createUser("owner-" + UUID.randomUUID());
        String ownerToken = loginAndGetToken(owner.getUsername(), "password123");
        String joinCode = objectMapper.readTree(
                postLeague(ownerToken, """
                        {
                            "name": "Office League"
                        }
                        """).getBody()
        ).get("joinCode").asText();

        String memberToken = loginAndGetToken(user.getUsername(), "password123");
        ResponseEntity<String> firstJoin = postJoin(memberToken, joinCode);
        ResponseEntity<String> secondJoin = postJoin(memberToken, joinCode);

        assertThat(firstJoin.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(secondJoin.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(leagueMemberRepository.count()).isEqualTo(2);
    }

    @Test
    void memberCanLeaveLeague() throws Exception {
        User owner = createUser("owner-" + UUID.randomUUID());
        String ownerToken = loginAndGetToken(owner.getUsername(), "password123");
        JsonNode league = objectMapper.readTree(
                postLeague(ownerToken, """
                        {
                            "name": "Office League"
                        }
                        """).getBody()
        );
        UUID leagueId = UUID.fromString(league.get("id").asText());
        String joinCode = league.get("joinCode").asText();

        String memberToken = loginAndGetToken(user.getUsername(), "password123");
        assertThat(postJoin(memberToken, joinCode).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> response = deleteMembership(memberToken, leagueId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(leagueMemberRepository.findByLeagueIdAndUserId(leagueId, user.getId()))
                .isEmpty();
        assertThat(leagueMemberRepository.findByLeagueIdAndUserId(leagueId, owner.getId()))
                .isPresent();
    }

    @Test
    void ownerCannotLeaveLeague() throws Exception {
        String ownerToken = loginAndGetToken(user.getUsername(), "password123");
        JsonNode league = objectMapper.readTree(
                postLeague(ownerToken, """
                        {
                            "name": "Office League"
                        }
                        """).getBody()
        );
        UUID leagueId = UUID.fromString(league.get("id").asText());

        ResponseEntity<String> response = deleteMembership(ownerToken, leagueId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("error").asText()).isEqualTo("OWNER_CANNOT_LEAVE");
        assertThat(leagueMemberRepository.findByLeagueIdAndUserId(leagueId, user.getId()))
                .isPresent();
    }

    @Test
    void unauthenticatedUserCannotLeaveLeague() throws Exception {
        User owner = createUser("owner-" + UUID.randomUUID());
        String ownerToken = loginAndGetToken(owner.getUsername(), "password123");
        UUID leagueId = UUID.fromString(
                objectMapper.readTree(
                        postLeague(ownerToken, """
                                {
                                    "name": "Office League"
                                }
                                """).getBody()
                ).get("id").asText()
        );

        ResponseEntity<String> response = deleteMembership(null, leagueId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(leagueMemberRepository.findAllByLeagueId(leagueId)).hasSize(1);
    }

    @Test
    void authenticatedUserCanRetrieveLeaguesTheyBelongTo() throws Exception {
        User owner = createUser("owner-" + UUID.randomUUID());
        String ownerToken = loginAndGetToken(owner.getUsername(), "password123");
        String memberToken = loginAndGetToken(user.getUsername(), "password123");

        JsonNode firstLeague = objectMapper.readTree(
                postLeague(ownerToken, """
                        {
                            "name": "First League"
                        }
                        """).getBody()
        );
        JsonNode secondLeague = objectMapper.readTree(
                postLeague(ownerToken, """
                        {
                            "name": "Second League"
                        }
                        """).getBody()
        );

        postJoin(memberToken, firstLeague.get("joinCode").asText());

        ResponseEntity<String> response = getLeagues(memberToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body).hasSize(1);
        assertThat(body.get(0).get("id").asText())
                .isEqualTo(firstLeague.get("id").asText());
        assertThat(body.get(0).get("name").asText()).isEqualTo("First League");
        assertThat(body.get(0).get("memberCount").asInt()).isEqualTo(2);
        assertThat(memberUsernames(body.get(0)))
                .containsExactlyInAnyOrder(owner.getUsername(), user.getUsername());
        assertThat(body.get(0).get("joinCode").asText())
                .isEqualTo(firstLeague.get("joinCode").asText());
        assertThat(body.get(0).get("id").asText())
                .isNotEqualTo(secondLeague.get("id").asText());
    }

    @Test
    void leagueMemberCanRetrieveLeagueDetails() throws Exception {
        User owner = createUser("owner-" + UUID.randomUUID());
        String ownerToken = loginAndGetToken(owner.getUsername(), "password123");
        JsonNode league = objectMapper.readTree(
                postLeague(ownerToken, """
                        {
                            "name": "Office League"
                        }
                        """).getBody()
        );
        UUID leagueId = UUID.fromString(league.get("id").asText());

        String memberToken = loginAndGetToken(user.getUsername(), "password123");
        postJoin(memberToken, league.get("joinCode").asText());

        ResponseEntity<String> response = getLeague(memberToken, leagueId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("id").asText()).isEqualTo(leagueId.toString());
        assertThat(body.get("name").asText()).isEqualTo("Office League");
        assertThat(body.get("ownerId").asText()).isEqualTo(owner.getId().toString());
        assertThat(body.get("memberCount").asInt()).isEqualTo(2);
        assertThat(memberUsernames(body))
                .containsExactlyInAnyOrder(owner.getUsername(), user.getUsername());
        assertThat(body.get("joinCode").asText()).isEqualTo(league.get("joinCode").asText());
    }

    @Test
    void nonMemberCannotRetrieveLeagueDetails() throws Exception {
        User owner = createUser("owner-" + UUID.randomUUID());
        String ownerToken = loginAndGetToken(owner.getUsername(), "password123");
        UUID leagueId = UUID.fromString(
                objectMapper.readTree(
                        postLeague(ownerToken, """
                                {
                                    "name": "Office League"
                                }
                                """).getBody()
                ).get("id").asText()
        );

        String token = loginAndGetToken(user.getUsername(), "password123");
        ResponseEntity<String> response = getLeague(token, leagueId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("error").asText()).isEqualTo("FORBIDDEN");
    }

    @Test
    void ownerCanUpdateLeagueName() throws Exception {
        String ownerToken = loginAndGetToken(user.getUsername(), "password123");
        UUID leagueId = UUID.fromString(
                objectMapper.readTree(
                        postLeague(ownerToken, """
                                {
                                    "name": "Office League"
                                }
                                """).getBody()
                ).get("id").asText()
        );

        ResponseEntity<String> response = patchLeague(
                ownerToken,
                leagueId,
                """
                        {
                            "name": "Renamed League"
                        }
                        """
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("name").asText()).isEqualTo("Renamed League");
        assertThat(leagueRepository.findById(leagueId))
                .hasValueSatisfying(league ->
                        assertThat(league.getName()).isEqualTo("Renamed League")
                );
    }

    @Test
    void nonOwnerCannotUpdateLeagueName() throws Exception {
        User owner = createUser("owner-" + UUID.randomUUID());
        String ownerToken = loginAndGetToken(owner.getUsername(), "password123");
        UUID leagueId = UUID.fromString(
                objectMapper.readTree(
                        postLeague(ownerToken, """
                                {
                                    "name": "Office League"
                                }
                                """).getBody()
                ).get("id").asText()
        );

        String memberToken = loginAndGetToken(user.getUsername(), "password123");
        ResponseEntity<String> response = patchLeague(
                memberToken,
                leagueId,
                """
                        {
                            "name": "Renamed League"
                        }
                        """
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(leagueRepository.findById(leagueId))
                .hasValueSatisfying(league ->
                        assertThat(league.getName()).isEqualTo("Office League")
                );
    }

    @Test
    void ownerCanRemoveMember() throws Exception {
        User owner = createUser("owner-" + UUID.randomUUID());
        String ownerToken = loginAndGetToken(owner.getUsername(), "password123");
        JsonNode league = objectMapper.readTree(
                postLeague(ownerToken, """
                        {
                            "name": "Office League"
                        }
                        """).getBody()
        );
        UUID leagueId = UUID.fromString(league.get("id").asText());

        String memberToken = loginAndGetToken(user.getUsername(), "password123");
        postJoin(memberToken, league.get("joinCode").asText());

        ResponseEntity<String> response = deleteMember(ownerToken, leagueId, user.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(leagueMemberRepository.findByLeagueIdAndUserId(leagueId, user.getId()))
                .isEmpty();
        assertThat(leagueMemberRepository.findByLeagueIdAndUserId(leagueId, owner.getId()))
                .isPresent();
    }

    @Test
    void nonOwnerCannotRemoveMember() throws Exception {
        User owner = createUser("owner-" + UUID.randomUUID());
        String ownerToken = loginAndGetToken(owner.getUsername(), "password123");
        JsonNode league = objectMapper.readTree(
                postLeague(ownerToken, """
                        {
                            "name": "Office League"
                        }
                        """).getBody()
        );
        UUID leagueId = UUID.fromString(league.get("id").asText());

        String memberToken = loginAndGetToken(user.getUsername(), "password123");
        postJoin(memberToken, league.get("joinCode").asText());

        ResponseEntity<String> response = deleteMember(memberToken, leagueId, owner.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(leagueMemberRepository.findAllByLeagueId(leagueId)).hasSize(2);
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

    private List<String> memberUsernames(JsonNode league) {
        return StreamSupport.stream(
                        league.get("memberUsernames").spliterator(),
                        false
                )
                .map(JsonNode::asText)
                .toList();
    }

    private ResponseEntity<String> postJoin(String token, String joinCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (token != null) {
            headers.setBearerAuth(token);
        }

        String requestBody = """
                {
                    "joinCode": "%s"
                }
                """.formatted(joinCode);

        return restTemplate.exchange(
                "/api/leagues/join",
                HttpMethod.POST,
                new HttpEntity<>(requestBody, headers),
                String.class
        );
    }

    private ResponseEntity<String> getLeagues(String token) {
        HttpHeaders headers = new HttpHeaders();

        if (token != null) {
            headers.setBearerAuth(token);
        }

        return restTemplate.exchange(
                "/api/leagues",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
    }

    private ResponseEntity<String> getLeague(String token, UUID leagueId) {
        HttpHeaders headers = new HttpHeaders();

        if (token != null) {
            headers.setBearerAuth(token);
        }

        return restTemplate.exchange(
                "/api/leagues/" + leagueId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
    }

    private ResponseEntity<String> patchLeague(
            String token,
            UUID leagueId,
            String requestBody
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (token != null) {
            headers.setBearerAuth(token);
        }

        return restTemplate.exchange(
                "/api/leagues/" + leagueId,
                HttpMethod.PATCH,
                new HttpEntity<>(requestBody, headers),
                String.class
        );
    }

    private ResponseEntity<String> deleteMembership(String token, UUID leagueId) {
        HttpHeaders headers = new HttpHeaders();

        if (token != null) {
            headers.setBearerAuth(token);
        }

        return restTemplate.exchange(
                "/api/leagues/" + leagueId + "/membership",
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                String.class
        );
    }

    private ResponseEntity<String> deleteMember(
            String token,
            UUID leagueId,
            UUID memberId
    ) {
        HttpHeaders headers = new HttpHeaders();

        if (token != null) {
            headers.setBearerAuth(token);
        }

        return restTemplate.exchange(
                "/api/leagues/" + leagueId + "/members/" + memberId,
                HttpMethod.DELETE,
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
