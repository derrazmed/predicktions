package com.ven.predicktions.integration.sports.footballdata;

import java.time.Instant;

public record FootballDataMatch(
        Long id,
        Instant utcDate,
        String status,
        FootballDataTeam homeTeam,
        FootballDataTeam awayTeam,
        FootballDataScore score
) {}