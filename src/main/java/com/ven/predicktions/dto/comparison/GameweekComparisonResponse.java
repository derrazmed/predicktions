package com.ven.predicktions.dto.comparison;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GameweekComparisonResponse(
        UUID leagueId,
        String leagueName,
        String competition,
        int season,
        int gameweek,
        String label,
        Instant firstKickoffAt,
        Instant lastKickoffAt,
        List<ComparisonMemberResponse> members,
        List<ComparisonMatchResponse> matches
) {
}
