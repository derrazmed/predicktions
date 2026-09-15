package com.ven.predicktions.controller;

import com.ven.predicktions.dto.prediction.AdminPredictionPageResponse;
import com.ven.predicktions.service.AdminPredictionService;
import com.ven.predicktions.service.AdminMatchService;
import com.ven.predicktions.dto.match.UpdateMatchResultRequest;
import com.ven.predicktions.dto.match.MatchResponse;
import com.ven.predicktions.dto.match.MatchRecalculationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/matches")
public class AdminMatchController {

    private final AdminPredictionService adminPredictionService;
    private final AdminMatchService adminMatchService;

    public AdminMatchController(
            AdminPredictionService adminPredictionService,
            AdminMatchService adminMatchService
    ) {
        this.adminPredictionService = adminPredictionService;
        this.adminMatchService = adminMatchService;
    }

    @GetMapping("/{matchId}/predictions")
    public ResponseEntity<AdminPredictionPageResponse> getMatchPredictions(
            @PathVariable UUID matchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                adminPredictionService.getMatchPredictions(matchId, page, size)
        );
    }

    @PatchMapping("/{matchId}/result")
    public ResponseEntity<MatchResponse> updateResult(
            @PathVariable UUID matchId,
            @Valid @RequestBody UpdateMatchResultRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(adminMatchService.updateResult(
                matchId,
                request,
                (UUID) authentication.getPrincipal()
        ));
    }

    @PostMapping("/{matchId}/recalculate")
    public ResponseEntity<MatchRecalculationResponse> recalculate(
            @PathVariable UUID matchId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(adminMatchService.recalculate(
                matchId,
                (UUID) authentication.getPrincipal()
        ));
    }
}
