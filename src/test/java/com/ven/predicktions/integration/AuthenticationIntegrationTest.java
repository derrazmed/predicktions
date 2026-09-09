package com.ven.predicktions.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "jwt.secret=test-secret-key-that-is-at-least-32-bytes-long-123456",
                "jwt.expiration=3600000"
        }
)
class AuthenticationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void shouldCompleteRegistrationLoginAndCurrentUserFlow() {
        // Registration
        Map<String, Object> registrationRequest = Map.of(
                "username", "integration-user",
                "email", "integration@example.com",
                "password", "Password123!"
        );

        ResponseEntity<Map> registrationResponse =
                restTemplate.postForEntity(
                        baseUrl() + "/api/auth/register",
                        registrationRequest,
                        Map.class
                );

        assertEquals(
                HttpStatus.CREATED,
                registrationResponse.getStatusCode()
        );

        assertNotNull(registrationResponse.getBody());
        assertEquals(
                "integration-user",
                registrationResponse.getBody().get("username")
        );

        assertNull(
                registrationResponse.getBody().get("password")
        );

        assertNull(
                registrationResponse.getBody().get("passwordHash")
        );

        // Login
        Map<String, Object> loginRequest = Map.of(
                "username", "integration-user",
                "password", "Password123!"
        );

        ResponseEntity<Map> loginResponse =
                restTemplate.postForEntity(
                        baseUrl() + "/api/auth/login",
                        loginRequest,
                        Map.class
                );

        assertEquals(
                HttpStatus.OK,
                loginResponse.getStatusCode()
        );

        assertNotNull(loginResponse.getBody());

        String accessToken =
                (String) loginResponse.getBody().get("accessToken");

        assertNotNull(accessToken);
        assertFalse(accessToken.isBlank());

        assertEquals(
                "Bearer",
                loginResponse.getBody().get("tokenType")
        );

        // Current user
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> currentUserResponse =
                restTemplate.exchange(
                        baseUrl() + "/api/users/me",
                        HttpMethod.GET,
                        request,
                        Map.class
                );

        assertEquals(
                HttpStatus.OK,
                currentUserResponse.getStatusCode()
        );

        assertNotNull(currentUserResponse.getBody());

        assertEquals(
                "integration-user",
                currentUserResponse.getBody().get("username")
        );

        assertEquals(
                "integration@example.com",
                currentUserResponse.getBody().get("email")
        );

        assertNotNull(
                currentUserResponse.getBody().get("id")
        );

        assertNull(
                currentUserResponse.getBody().get("password")
        );

        assertNull(
                currentUserResponse.getBody().get("passwordHash")
        );
    }
}