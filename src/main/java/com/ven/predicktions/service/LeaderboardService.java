package com.ven.predicktions.service;

import com.ven.predicktions.dto.leaderboard.LeaderboardEntryResponse;

import java.util.List;

public interface LeaderboardService {

    List<LeaderboardEntryResponse> getGlobalLeaderboard();
}