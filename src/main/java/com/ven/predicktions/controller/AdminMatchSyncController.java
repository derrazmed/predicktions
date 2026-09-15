package com.ven.predicktions.controller;

import com.ven.predicktions.config.SportsSyncProperties;
import com.ven.predicktions.dto.match.MatchSyncRequest;
import com.ven.predicktions.dto.match.MatchSyncResponse;
import com.ven.predicktions.service.MatchSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/matches")
public class AdminMatchSyncController {

    private final MatchSyncService matchSyncService;
    private final SportsSyncProperties properties;

    public AdminMatchSyncController(
            MatchSyncService matchSyncService,
            SportsSyncProperties properties
    ) {
        this.matchSyncService = matchSyncService;
        this.properties = properties;
    }

    @PostMapping("/sync")
    public ResponseEntity<MatchSyncResponse> synchronize(
            @RequestParam(required = false) Integer gameweek,
            @RequestParam(required = false) String competition,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        return ResponseEntity.ok(matchSyncService.synchronizeManually(
                new MatchSyncRequest(
                        competition == null || competition.isBlank()
                                ? properties.getCompetitionCode()
                                : competition,
                        gameweek,
                        date,
                        from,
                        to
                )
        ));
    }
}
