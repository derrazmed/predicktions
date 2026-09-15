package com.ven.predicktions.dto.league;

import java.util.List;

public record AdminLeaguePageResponse(
        List<AdminLeagueResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
