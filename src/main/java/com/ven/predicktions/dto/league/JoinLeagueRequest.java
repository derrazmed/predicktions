package com.ven.predicktions.dto.league;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinLeagueRequest(

        @NotBlank(message = "joinCode is required")
        @Size(max = 32, message = "joinCode must be at most 32 characters")
        String joinCode
) {
}
