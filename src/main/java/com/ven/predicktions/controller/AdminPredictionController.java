package com.ven.predicktions.controller;

import com.ven.predicktions.dto.prediction.AdminPredictionPageResponse;
import com.ven.predicktions.service.AdminPredictionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/predictions")
public class AdminPredictionController {

    private final AdminPredictionService adminPredictionService;

    public AdminPredictionController(AdminPredictionService adminPredictionService) {
        this.adminPredictionService = adminPredictionService;
    }

    @GetMapping
    public ResponseEntity<AdminPredictionPageResponse> getPredictions(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID matchId,
            @RequestParam(required = false) Integer gameweek,
            @RequestParam(required = false) UUID leagueId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminPredictionService.getPredictions(
                userId, matchId, gameweek, leagueId, fromDate, toDate, page, size
        ));
    }
}
