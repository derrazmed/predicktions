package com.ven.predicktions.dto.match;

import java.time.Instant;
import java.util.UUID;

public record MatchRecalculationResponse(
        UUID matchId,
        Integer homeScore,
        Integer awayScore,
        int predictionsProcessed,
        int predictionsUpdated,
        int predictionsUnchanged,
        Instant recalculatedAt
) {
}
