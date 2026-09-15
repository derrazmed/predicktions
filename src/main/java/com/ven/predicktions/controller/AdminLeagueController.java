package com.ven.predicktions.controller;

import com.ven.predicktions.dto.league.AdminLeaguePageResponse;
import com.ven.predicktions.service.AdminLeagueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/leagues")
public class AdminLeagueController {

    private final AdminLeagueService adminLeagueService;

    public AdminLeagueController(AdminLeagueService adminLeagueService) {
        this.adminLeagueService = adminLeagueService;
    }

    @GetMapping
    public ResponseEntity<AdminLeaguePageResponse> getLeagues(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminLeagueService.getLeagues(page, size));
    }
}
