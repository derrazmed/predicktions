package com.ven.predicktions.dto.prediction;

import java.time.Instant;
import java.util.UUID;

public record AdminPredictionResponse(
        UUID id,
        UUID userId,
        String username,
        UUID matchId,
        Integer gameweek,
        Integer predictedHomeScore,
        Integer predictedAwayScore,
        Integer points,
        Instant createdAt,
        Instant updatedAt
) {
}
