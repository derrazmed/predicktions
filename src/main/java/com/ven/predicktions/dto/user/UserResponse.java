package com.ven.predicktions.dto.user;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email
) {}