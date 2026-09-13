package com.ven.predicktions.security;

import com.ven.predicktions.PredicktionApplication;
import com.ven.predicktions.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        classes = PredicktionApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(SecurityConfigTest.TestAdminController.class)
class SecurityConfigTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldRejectUnauthenticatedProtectedEndpoint() {
        String url = "http://localhost:" + port + "/api/users/me";

        ResponseEntity<String> response = restTemplate.getForEntity(
                url,
                String.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldRejectUnauthenticatedApiEndpoint() {
        String url = "http://localhost:" + port + "/api/something-protected";

        ResponseEntity<String> response = restTemplate.getForEntity(
                url,
                String.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldRejectUnauthenticatedAdminEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                adminUrl(),
                String.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldRejectUserFromAdminEndpoint() {
        ResponseEntity<String> response = restTemplate.exchange(
                adminUrl(),
                org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(bearerToken(Role.USER)),
                String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void shouldAllowAdminThroughAdminAuthorizationRule() {
        ResponseEntity<String> response = restTemplate.exchange(
                adminUrl(),
                org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(bearerToken(Role.ADMIN)),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private String adminUrl() {
        return "http://localhost:" + port + "/api/admin/security-test";
    }

    private org.springframework.http.HttpHeaders bearerToken(Role role) {
        org.springframework.http.HttpHeaders headers =
                new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(jwtService.generateToken(UUID.randomUUID(), role));
        return headers;
    }

    @RestController
    static class TestAdminController {

        @GetMapping("/api/admin/security-test")
        String securityTest() {
            return "ok";
        }
    }
}