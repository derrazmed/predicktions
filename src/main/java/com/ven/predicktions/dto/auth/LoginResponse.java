package com.ven.predicktions.dto.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {}