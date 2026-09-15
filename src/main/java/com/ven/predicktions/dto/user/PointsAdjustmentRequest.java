package com.ven.predicktions.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;

public record PointsAdjustmentRequest(
        @NotNull(message = "Points are required")
        @Min(value = -1_000_000, message = "Points must be between -1000000 and 1000000")
        @Max(value = 1_000_000, message = "Points must be between -1000000 and 1000000")
        Integer points,
        @NotBlank(message = "Reason is required")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {
    @AssertTrue(message = "Points must not be zero")
    @JsonIgnore
    public boolean isNonZero() {
        return points != null && points != 0;
    }
}
