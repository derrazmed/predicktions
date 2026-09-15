package com.ven.predicktions.dto.league;

import com.ven.predicktions.model.Role;

import java.time.Instant;
import java.util.UUID;

public record AdminLeagueMemberResponse(
        UUID userId,
        String username,
        Instant joinedAt,
        Role role
) {
}
