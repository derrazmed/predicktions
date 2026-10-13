package com.ven.predicktions.dto.comparison;

import com.ven.predicktions.model.MatchStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ComparisonMatchResponse(
        UUID matchId,
        String homeTeam,
        String awayTeam,
        Instant kickoffAt,
        Integer homeScore,
        Integer awayScore,
        MatchStatus status,
        List<MemberPredictionResponse> predictions
) {
}
