package com.ven.predicktions.dto.auth;

import com.ven.predicktions.model.Role;

import java.time.Instant;
import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String username,
        String email,
        Instant createdAt,
        Role role
) {
    public RegisterResponse(
            UUID id,
            String username,
            String email,
            Instant createdAt
    ) {
        this(id, username, email, createdAt, Role.USER);
    }
}