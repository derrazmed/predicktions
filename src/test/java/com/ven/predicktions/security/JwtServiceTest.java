package com.ven.predicktions.security;

import com.ven.predicktions.config.JwtProperties;
import com.ven.predicktions.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "TiYcfiJQMYlDt/hsuTlwXgkhhxhRY8Dyxp4sWqcri++90p7L2wQRb9W6f8CbxKAXLMsyO7USqk5GsdhKOXfbmA==",
                3600000
        );

        jwtService = new JwtService(properties);
    }

    @Test
    void shouldGenerateAndValidateToken() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateToken(userId);

        assertNotNull(token);
        assertTrue(jwtService.isValid(token));
        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals(Role.USER, jwtService.extractRole(token));
    }

    @Test
    void shouldPreserveAdminRoleInSignedToken() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateToken(userId, Role.ADMIN);

        assertTrue(jwtService.isValid(token));
        assertEquals(Role.ADMIN, jwtService.extractRole(token));
    }

    @Test
    void shouldRejectInvalidToken() {
        assertFalse(jwtService.isValid("invalid-token"));
    }

    @Test
    void shouldRejectTokenSignedWithDifferentKey() {
        UUID userId = UUID.randomUUID();

        JwtService otherJwtService = new JwtService(
                new JwtProperties(
                        "another-test-secret-that-is-at-least-32-bytes-long-123456789",
                        3600000
                )
        );

        String token = otherJwtService.generateToken(userId);

        assertFalse(jwtService.isValid(token));
    }
}