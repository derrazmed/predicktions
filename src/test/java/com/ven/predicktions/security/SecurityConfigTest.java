package com.ven.predicktions.security;

import com.ven.predicktions.PredicktionApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        classes = PredicktionApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class SecurityConfigTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

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
}