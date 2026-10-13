package com.ven.predicktions.service;

import com.ven.predicktions.dto.comparison.GameweekComparisonResponse;
import com.ven.predicktions.dto.comparison.GameweekSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface LeagueComparisonService {

    List<GameweekSummaryResponse> getCompletedGameweeks(UUID userId, UUID leagueId);

    GameweekComparisonResponse getGameweekComparison(
            UUID userId,
            UUID leagueId,
            int season,
            int gameweek,
            UUID memberId
    );
}
