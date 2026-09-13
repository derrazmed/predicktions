package com.ven.predicktions.controller;

import com.ven.predicktions.dto.league.CreateLeagueRequest;
import com.ven.predicktions.dto.league.JoinLeagueRequest;
import com.ven.predicktions.dto.league.LeagueResponse;
import com.ven.predicktions.dto.league.UpdateLeagueRequest;
import com.ven.predicktions.service.LeagueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leagues")
public class LeagueController {

    private final LeagueService leagueService;

    public LeagueController(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeagueResponse createLeague(
            @Valid @RequestBody CreateLeagueRequest request,
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        return leagueService.createLeague(userId, request);
    }

    @GetMapping
    public List<LeagueResponse> getLeagues(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();

        return leagueService.getLeaguesForUser(userId);
    }

    @GetMapping("/{leagueId}")
    public LeagueResponse getLeague(
            @PathVariable UUID leagueId,
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        return leagueService.getLeague(userId, leagueId);
    }

    @PatchMapping("/{leagueId}")
    public LeagueResponse updateLeague(
            @PathVariable UUID leagueId,
            @Valid @RequestBody UpdateLeagueRequest request,
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        return leagueService.updateLeague(userId, leagueId, request);
    }

    @PostMapping("/join")
    @ResponseStatus(HttpStatus.CREATED)
    public LeagueResponse joinLeague(
            @Valid @RequestBody JoinLeagueRequest request,
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        return leagueService.joinLeague(userId, request);
    }

    @DeleteMapping("/{leagueId}/membership")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveLeague(
            @PathVariable UUID leagueId,
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        leagueService.leaveLeague(userId, leagueId);
    }

    @DeleteMapping("/{leagueId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable UUID leagueId,
            @PathVariable("userId") UUID memberId,
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        leagueService.removeMember(userId, leagueId, memberId);
    }
}
