package com.ven.predicktions.controller;

import com.ven.predicktions.dto.user.AdminPointsAdjustmentPageResponse;
import com.ven.predicktions.dto.user.AdjustmentHistoryFilter;
import com.ven.predicktions.service.AdminPointsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/points")
public class AdminPointsController {

    private final AdminPointsService adminPointsService;

    public AdminPointsController(AdminPointsService adminPointsService) {
        this.adminPointsService = adminPointsService;
    }

    @GetMapping("/adjustments")
    public ResponseEntity<AdminPointsAdjustmentPageResponse> getAdjustments(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String adminId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Instant parsedFrom = parseInstant(from, "from");
        Instant parsedTo = parseInstant(to, "to");
        if (parsedFrom != null && parsedTo != null && parsedFrom.isAfter(parsedTo)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        return ResponseEntity.ok(adminPointsService.getAllAdjustmentHistory(
                new AdjustmentHistoryFilter(
                        parseUuid(userId, "userId"),
                        parseUuid(adminId, "adminId"),
                        parsedFrom,
                        parsedTo
                ),
                page,
                size
        ));
    }

    private UUID parseUuid(String value, String parameter) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(parameter + " must be a valid UUID");
        }
    }

    private Instant parseInstant(String value, String parameter) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    parameter + " must be a valid ISO-8601 timestamp"
            );
        }
    }
}
