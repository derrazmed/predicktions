package com.ven.predicktions.dto.league;

import com.ven.predicktions.dto.leaderboard.LeaderboardEntryResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminLeagueDetailsResponse(
        UUID id,
        String name,
        Instant createdAt,
        String joinCode,
        long memberCount,
        AdminLeagueOwnerResponse owner,
        List<AdminLeagueMemberResponse> members,
        List<LeaderboardEntryResponse> leaderboard,
        AdminLeagueStatisticsResponse statistics
) {
}
