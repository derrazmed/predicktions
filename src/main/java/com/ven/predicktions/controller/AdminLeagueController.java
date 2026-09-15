package com.ven.predicktions.controller;

import com.ven.predicktions.dto.league.AdminLeaguePageResponse;
import com.ven.predicktions.dto.league.AdminLeagueDetailsResponse;
import com.ven.predicktions.service.AdminLeagueService;
import com.ven.predicktions.service.AdminLeagueDetailsService;
import com.ven.predicktions.service.AdminLeagueDeletionService;
import com.ven.predicktions.service.AdminLeagueMembershipService;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/leagues")
public class AdminLeagueController {

    private final AdminLeagueService adminLeagueService;
    private final AdminLeagueDetailsService adminLeagueDetailsService;
    private final AdminLeagueDeletionService adminLeagueDeletionService;
    private final AdminLeagueMembershipService adminLeagueMembershipService;

    public AdminLeagueController(
            AdminLeagueService adminLeagueService,
            AdminLeagueDetailsService adminLeagueDetailsService,
            AdminLeagueDeletionService adminLeagueDeletionService,
            AdminLeagueMembershipService adminLeagueMembershipService
    ) {
        this.adminLeagueService = adminLeagueService;
        this.adminLeagueDetailsService = adminLeagueDetailsService;
        this.adminLeagueDeletionService = adminLeagueDeletionService;
        this.adminLeagueMembershipService = adminLeagueMembershipService;
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

    @DeleteMapping("/{leagueId}")
    public ResponseEntity<Void> deleteLeague(
            @PathVariable UUID leagueId,
            Authentication authentication
    ) {
        adminLeagueDeletionService.deleteLeague(
                leagueId,
                (UUID) authentication.getPrincipal()
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{leagueId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID leagueId,
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        adminLeagueMembershipService.removeMember(
                leagueId,
                userId,
                (UUID) authentication.getPrincipal()
        );
        return ResponseEntity.noContent().build();
    }
}
