package com.ven.predicktions.service;

import com.ven.predicktions.dto.leaderboard.LeaderboardEntryResponse;
import com.ven.predicktions.dto.leaderboard.LeaderboardRecalculationResponse;

import java.util.List;
import java.util.UUID;

public interface LeaderboardService {

    List<LeaderboardEntryResponse> getGlobalLeaderboard();

    List<LeaderboardEntryResponse> getGlobalLeaderboard(Integer gameweek, Integer season);

    LeaderboardRecalculationResponse recalculateGlobalLeaderboard(UUID adminId);

    List<LeaderboardEntryResponse> getLeagueLeaderboard(UUID userId, UUID leagueId);

    List<LeaderboardEntryResponse> getLeagueLeaderboardForAdmin(UUID leagueId);
}