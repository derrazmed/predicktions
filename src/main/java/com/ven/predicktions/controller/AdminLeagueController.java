package com.ven.predicktions.controller;

import com.ven.predicktions.dto.league.AdminLeaguePageResponse;
import com.ven.predicktions.dto.league.AdminLeagueDetailsResponse;
import com.ven.predicktions.service.AdminLeagueService;
import com.ven.predicktions.service.AdminLeagueDetailsService;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/leagues")
public class AdminLeagueController {

    private final AdminLeagueService adminLeagueService;
    private final AdminLeagueDetailsService adminLeagueDetailsService;

    public AdminLeagueController(
            AdminLeagueService adminLeagueService,
            AdminLeagueDetailsService adminLeagueDetailsService
    ) {
        this.adminLeagueService = adminLeagueService;
        this.adminLeagueDetailsService = adminLeagueDetailsService;
    }

    @GetMapping
    public ResponseEntity<AdminLeaguePageResponse> getLeagues(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminLeagueService.getLeagues(page, size));
    }

    @GetMapping("/{leagueId}")
    public ResponseEntity<AdminLeagueDetailsResponse> getLeague(
            @PathVariable UUID leagueId
    ) {
        return ResponseEntity.ok(adminLeagueDetailsService.getLeague(leagueId));
    }
}
