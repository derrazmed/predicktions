package com.ven.predicktions.dto.user;

import com.ven.predicktions.model.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeUserRoleRequest(
        @NotNull(message = "Role is required")
        Role role
) {
}
