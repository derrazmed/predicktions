package com.ven.predicktions.dto.match;

import java.time.LocalDate;

public record MatchSyncRequest(
        String competition,
        Integer gameweek,
        LocalDate date,
        LocalDate from,
        LocalDate to
) {
    public MatchSyncRequest {
        if (date != null && (from != null || to != null)) {
            throw new IllegalArgumentException("date cannot be combined with from or to");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        if (gameweek != null && (gameweek < 1 || gameweek > 53)) {
            throw new IllegalArgumentException("gameweek must be between 1 and 53");
        }
    }
}
