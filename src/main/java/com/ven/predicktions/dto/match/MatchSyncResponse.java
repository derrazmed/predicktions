package com.ven.predicktions.dto.match;

import java.time.Instant;

public record MatchSyncResponse(
        int matchesRetrieved,
        int matchesCreated,
        int matchesUpdated,
        int resultsUpdated,
        int failed,
        Instant synchronizedAt
) {
}
