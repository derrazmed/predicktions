package com.ven.predicktions.dto.leaderboard;

import java.time.Instant;

public record LeaderboardRecalculationResponse(
        Instant recalculatedAt,
        int usersProcessed,
        int entriesUpdated,
        int entriesUnchanged,
        String message
) {
}
