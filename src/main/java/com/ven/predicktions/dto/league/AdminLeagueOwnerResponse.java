package com.ven.predicktions.dto.league;

import java.util.UUID;

public record AdminLeagueOwnerResponse(
        UUID id,
        String username
) {
}
