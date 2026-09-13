package com.ven.predicktions.dto.leaderboard;

import java.util.UUID;

public record LeaderboardEntryResponse(
        int rank,
        UUID userId,
        String username,
        long totalPoints
) {
}