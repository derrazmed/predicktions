package com.ven.predicktions.dto.comparison;

import java.util.UUID;

public record MemberPredictionResponse(
        UUID userId,
        String username,
        Integer predictedHomeScore,
        Integer predictedAwayScore,
        Integer points,
        PredictionOutcome outcome
) {
}
