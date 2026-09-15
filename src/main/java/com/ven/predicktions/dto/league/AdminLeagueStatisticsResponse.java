package com.ven.predicktions.dto.league;

public record AdminLeagueStatisticsResponse(
        long totalPredictions,
        long predictionsWithPoints,
        long totalPointsAwarded,
        double averagePointsPerPrediction,
        long exactScorePredictions,
        long participatingMembers
) {
}
