package com.ven.predicktions.dto.prediction;

import java.time.Instant;
import java.util.UUID;

public record PredictionResponse(
        UUID id,
        UUID matchId,
        Integer predictedHomeScore,
        Integer predictedAwayScore,
        Integer points,
        Instant createdAt,
        Instant updatedAt
) {
}