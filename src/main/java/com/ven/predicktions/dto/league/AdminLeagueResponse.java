package com.ven.predicktions.dto.league;

import java.time.Instant;
import java.util.UUID;

public record AdminLeagueResponse(
        UUID id,
        String name,
        AdminLeagueOwnerResponse owner,
        long memberCount,
        Instant createdAt,
        String joinCode
) {
}
