package com.ven.predicktions.dto.league;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LeagueResponse(
        UUID id,
        String name,
        UUID ownerId,
        long memberCount,
        List<String> memberUsernames,
        String joinCode,
        Instant createdAt
) {
}
