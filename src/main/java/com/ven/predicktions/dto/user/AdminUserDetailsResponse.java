package com.ven.predicktions.dto.user;

import com.ven.predicktions.model.Role;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUserDetailsResponse(
        UUID id,
        String username,
        String email,
        Role role,
        Instant createdAt,
        boolean enabled,
        long predictionCount,
        long points,
        List<AdminLeagueSummary> leagues,
        Integer leaderboardPosition
) {
}
