package com.ven.predicktions.controller;

import com.ven.predicktions.dto.comparison.GameweekComparisonResponse;
import com.ven.predicktions.dto.comparison.GameweekSummaryResponse;
import com.ven.predicktions.service.LeagueComparisonService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leagues/{leagueId}/comparisons")
public class LeagueComparisonController {

    private final LeagueComparisonService leagueComparisonService;

    public LeagueComparisonController(LeagueComparisonService leagueComparisonService) {
        this.leagueComparisonService = leagueComparisonService;
    }

    @GetMapping("/gameweeks")
    public List<GameweekSummaryResponse> getCompletedGameweeks(
            @PathVariable UUID leagueId,
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        return leagueComparisonService.getCompletedGameweeks(userId, leagueId);
    }

    @GetMapping("/gameweeks/{season}/{gameweek}")
    public GameweekComparisonResponse getGameweekComparison(
            @PathVariable UUID leagueId,
            @PathVariable int season,
            @PathVariable int gameweek,
            @RequestParam(required = false) UUID memberId,
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        return leagueComparisonService.getGameweekComparison(
                userId,
                leagueId,
                season,
                gameweek,
                memberId
        );
    }
}
