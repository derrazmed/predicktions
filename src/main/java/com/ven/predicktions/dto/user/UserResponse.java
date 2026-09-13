package com.ven.predicktions.dto.user;

import com.ven.predicktions.model.Role;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        Role role
) {
    public UserResponse(UUID id, String username, String email) {
        this(id, username, email, Role.USER);
    }
}