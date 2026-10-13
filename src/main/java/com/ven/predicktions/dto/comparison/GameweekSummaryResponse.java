package com.ven.predicktions.dto.comparison;

import java.time.Instant;

public record GameweekSummaryResponse(
        int season,
        int gameweek,
        String label,
        int matchCount,
        Instant firstKickoffAt,
        Instant lastKickoffAt
) {
}
