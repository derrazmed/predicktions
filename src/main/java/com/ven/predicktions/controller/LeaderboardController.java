package com.ven.predicktions.controller;

import com.ven.predicktions.dto.leaderboard.LeaderboardEntryResponse;
import com.ven.predicktions.service.LeaderboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public List<LeaderboardEntryResponse> getGlobalLeaderboard() {
        return leaderboardService.getGlobalLeaderboard();
    }
}