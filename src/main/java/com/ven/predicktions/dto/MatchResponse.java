package com.ven.predicktions.dto;

import com.ven.predicktions.model.MatchStatus;

import java.time.Instant;
import java.util.UUID;

public record MatchResponse(
        UUID id,
        String externalId,
        String homeTeam,
        String awayTeam,
        Instant kickoffAt,
        Integer homeScore,
        Integer awayScore,
        MatchStatus status
) {
}
