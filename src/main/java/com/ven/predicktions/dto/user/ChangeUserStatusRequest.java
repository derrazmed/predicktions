package com.ven.predicktions.dto.user;

import jakarta.validation.constraints.NotNull;

public record ChangeUserStatusRequest(
        @NotNull(message = "Enabled is required")
        Boolean enabled
) {
}
