package com.ven.predicktions.dto.auth;

import java.time.Instant;
import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String username,
        String email,
        Instant createdAt
) {
}