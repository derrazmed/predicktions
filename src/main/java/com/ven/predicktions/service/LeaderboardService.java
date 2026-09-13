package com.ven.predicktions.service;

import com.ven.predicktions.dto.leaderboard.LeaderboardEntryResponse;

import java.util.List;
import java.util.UUID;

public interface LeaderboardService {

    List<LeaderboardEntryResponse> getGlobalLeaderboard();

    List<LeaderboardEntryResponse> getLeagueLeaderboard(UUID userId, UUID leagueId);
}