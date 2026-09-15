package com.ven.predicktions.dto.user;

import java.time.Instant;
import java.util.UUID;

public record PointsAdjustmentResponse(
        UUID userId,
        String username,
        int points,
        String reason,
        UUID awardedBy,
        Instant createdAt
) {
}
