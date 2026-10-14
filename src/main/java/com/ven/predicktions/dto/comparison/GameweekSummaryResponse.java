package com.ven.predicktions.dto.comparison;

import java.time.Instant;

public record GameweekSummaryResponse(
        String competition,
        int season,
        int gameweek,
        String label,
        int matchCount,
        int eligibleMatchCount,
        Instant firstKickoffAt,
        Instant lastKickoffAt
) {
}
