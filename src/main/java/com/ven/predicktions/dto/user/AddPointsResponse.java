package com.ven.predicktions.dto.user;

import java.time.Instant;
import java.util.UUID;

public record AddPointsResponse(
        UUID userId,
        String username,
        int pointsAwarded,
        String reason,
        UUID awardedBy,
        Instant createdAt
) {
}
