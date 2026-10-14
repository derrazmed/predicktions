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
        MatchStatus status,
        String competitionCode,
        Integer season,
        Integer matchday
) {

    /**
     * Provider-neutral match identity is optional so callers that only care
     * about fixtures/results (and tests) can omit it.
     */
    public SportsMatch(
            String externalId,
            String homeTeam,
            String awayTeam,
            Instant kickoffAt,
            Integer homeScore,
            Integer awayScore,
            MatchStatus status
    ) {
        this(
                externalId,
                homeTeam,
                awayTeam,
                kickoffAt,
                homeScore,
                awayScore,
                status,
                null,
                null,
                null
        );
    }
}
