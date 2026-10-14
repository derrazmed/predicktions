package com.ven.predicktions.service;

import com.ven.predicktions.dto.comparison.GameweekComparisonResponse;
import com.ven.predicktions.dto.comparison.GameweekSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface LeagueComparisonService {

    List<GameweekSummaryResponse> getGameweeks(UUID userId, UUID leagueId);

    GameweekComparisonResponse getGameweekComparison(
            UUID userId,
            UUID leagueId,
            String competition,
            int season,
            int gameweek,
            UUID memberId
    );
}
