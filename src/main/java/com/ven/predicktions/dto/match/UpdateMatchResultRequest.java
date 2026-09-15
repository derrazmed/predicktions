package com.ven.predicktions.dto.match;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateMatchResultRequest(
        @NotNull(message = "Home score is required")
        @Min(value = 0, message = "Home score cannot be negative")
        Integer homeScore,
        @NotNull(message = "Away score is required")
        @Min(value = 0, message = "Away score cannot be negative")
        Integer awayScore
) {
}
