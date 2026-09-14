package com.ven.predicktions.dto.user;

import com.ven.predicktions.model.Role;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String username,
        String email,
        Role role,
        Instant createdAt,
        boolean enabled
) {
}
