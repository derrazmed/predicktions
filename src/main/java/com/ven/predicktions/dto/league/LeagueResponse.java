package com.ven.predicktions.dto.league;

import java.time.Instant;
import java.util.UUID;

public record LeagueResponse(
        UUID id,
        String name,
        UUID ownerId,
        String joinCode,
        Instant createdAt
) {
}
