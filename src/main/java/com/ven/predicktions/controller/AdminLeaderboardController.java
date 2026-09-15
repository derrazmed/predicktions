package com.ven.predicktions.controller;

import com.ven.predicktions.dto.leaderboard.LeaderboardEntryResponse;
import com.ven.predicktions.dto.leaderboard.LeaderboardRecalculationResponse;
import com.ven.predicktions.service.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/leaderboard")
public class AdminLeaderboardController {

    private final LeaderboardService leaderboardService;

    public AdminLeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public ResponseEntity<List<LeaderboardEntryResponse>> getGlobalLeaderboard(
            @RequestParam(required = false) Integer gameweek,
            @RequestParam(required = false) Integer season
    ) {
        return ResponseEntity.ok(
                leaderboardService.getGlobalLeaderboard(gameweek, season)
        );
    }

    @PostMapping("/recalculate")
    public ResponseEntity<LeaderboardRecalculationResponse> recalculate(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                leaderboardService.recalculateGlobalLeaderboard(
                        (java.util.UUID) authentication.getPrincipal()
                )
        );
    }
}
