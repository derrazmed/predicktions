package com.ven.predicktions.integration.sports;

import com.ven.predicktions.model.MatchStatus;

import java.time.Instant;

public record SportsMatch(
        String externalId,
        String homeTeam,
        String awayTeam,
        Instant kickoffAt,
        Integer homeScore,
        Integer awayScore,
        MatchStatus status
) {
}