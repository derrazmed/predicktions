package com.ven.predicktions.dto.user;

import java.time.Instant;
import java.util.UUID;

public record AdminPointsAdjustmentResponse(
        UUID id,
        UUID userId,
        UUID adminId,
        int points,
        String reason,
        Instant createdAt
) {
}
