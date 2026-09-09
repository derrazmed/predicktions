package com.ven.predicktions.dto.prediction;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePredictionRequest(

        @NotNull(message = "matchId is required")
        UUID matchId,

        @NotNull(message = "predictedHomeScore is required")
        @Min(value = 0, message = "predictedHomeScore must be non-negative")
        Integer predictedHomeScore,

        @NotNull(message = "predictedAwayScore is required")
        @Min(value = 0, message = "predictedAwayScore must be non-negative")
        Integer predictedAwayScore
) {
}