package com.ven.predicktions.security;

import com.ven.predicktions.PredicktionApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(
        classes = PredicktionApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "jwt.secret=test-secret-key-that-is-at-least-32-bytes-long-123456",
                "jwt.expiration=3600000",
                "app.cors.allowed-origins=http://localhost:5173,https://predicktions-frontend.onrender.com"
        }
)
class CorsConfigTest {

    private static final String ALLOWED_ORIGIN =
            "https://predicktions-frontend.onrender.com";
    private static final String DISALLOWED_ORIGIN =
            "https://evil.example.com";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpEntity<Void> preflight(String origin) {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin(origin);
        headers.setAccessControlRequestMethod(HttpMethod.POST);
        headers.setAccessControlRequestHeaders(List.of("content-type", "authorization"));
        return new HttpEntity<>(headers);
    }

    @Test
    void shouldAnswerPreflightRequestForAllowedOrigin() {
        ResponseEntity<Void> response = restTemplate.exchange(
                url("/api/auth/login"),
                HttpMethod.OPTIONS,
                preflight(ALLOWED_ORIGIN),
                Void.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                ALLOWED_ORIGIN,
                response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)
        );
        assertEquals(
                List.of("content-type", "authorization"),
                response.getHeaders().getAccessControlAllowHeaders()
        );
        assertTrue(
                response.getHeaders().getAccessControlAllowMethods().contains(HttpMethod.POST),
                "preflight must advertise POST as an allowed method"
        );
    }

    @Test
    void shouldNotAnswerPreflightRequestForDisallowedOrigin() {
        ResponseEntity<Void> response = restTemplate.exchange(
                url("/api/auth/login"),
                HttpMethod.OPTIONS,
                preflight(DISALLOWED_ORIGIN),
                Void.class
        );

        assertNull(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void shouldKeepInvalidCredentialsRejectedButReadableFromAllowedOrigin() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setOrigin(ALLOWED_ORIGIN);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/auth/login"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of("username", "nobody", "password", "wrong-password"), headers),
                String.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(
                ALLOWED_ORIGIN,
                response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)
        );
    }

    @Test
    void shouldNotAddCorsHeaderForDisallowedOrigin() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setOrigin(DISALLOWED_ORIGIN);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/auth/login"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of("username", "nobody", "password", "wrong-password"), headers),
                String.class
        );

        assertNull(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
