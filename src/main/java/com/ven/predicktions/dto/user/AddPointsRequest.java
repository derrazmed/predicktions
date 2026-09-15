package com.ven.predicktions.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AddPointsRequest(
        @NotNull(message = "Points are required")
        @Positive(message = "Points must be positive")
        Integer points,
        @NotBlank(message = "Reason is required")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {
}
