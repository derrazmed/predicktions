package com.ven.predicktions.controller;

import com.ven.predicktions.dto.prediction.AdminPredictionPageResponse;
import com.ven.predicktions.service.AdminPredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/matches")
public class AdminMatchController {

    private final AdminPredictionService adminPredictionService;

    public AdminMatchController(AdminPredictionService adminPredictionService) {
        this.adminPredictionService = adminPredictionService;
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
}
