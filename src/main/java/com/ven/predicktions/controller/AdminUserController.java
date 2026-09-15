package com.ven.predicktions.controller;

import com.ven.predicktions.dto.user.AdminUserPageResponse;
import com.ven.predicktions.dto.user.AdminUserDetailsResponse;
import com.ven.predicktions.dto.user.AdminUserResponse;
import com.ven.predicktions.dto.user.ChangeUserRoleRequest;
import com.ven.predicktions.dto.user.ChangeUserStatusRequest;
import com.ven.predicktions.dto.user.PointsAdjustmentRequest;
import com.ven.predicktions.dto.user.PointsAdjustmentResponse;
import com.ven.predicktions.dto.prediction.AdminPredictionPageResponse;
import com.ven.predicktions.service.AdminPointsService;
import com.ven.predicktions.service.AdminPredictionService;
import com.ven.predicktions.service.AdminUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AdminPredictionService adminPredictionService;
    private final AdminPointsService adminPointsService;

    public AdminUserController(
            AdminUserService adminUserService,
            AdminPredictionService adminPredictionService,
            AdminPointsService adminPointsService
    ) {
        this.adminUserService = adminUserService;
        this.adminPredictionService = adminPredictionService;
        this.adminPointsService = adminPointsService;
    }

    @GetMapping
    public ResponseEntity<AdminUserPageResponse> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminUserService.getUsers(page, size));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserDetailsResponse> getUser(
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(adminUserService.getUser(userId));
    }

    @GetMapping("/{userId}/predictions")
    public ResponseEntity<AdminPredictionPageResponse> getUserPredictions(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                adminPredictionService.getUserPredictions(userId, page, size)
        );
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<AdminUserResponse> changeUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeUserRoleRequest request
    ) {
        return ResponseEntity.ok(
                adminUserService.changeUserRole(userId, request.role())
        );
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<AdminUserResponse> changeUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeUserStatusRequest request
    ) {
        return ResponseEntity.ok(
                adminUserService.setUserEnabled(userId, request.enabled())
        );
    }

    @PostMapping("/{userId}/points")
    public ResponseEntity<PointsAdjustmentResponse> adjustPoints(
            @PathVariable UUID userId,
            @Valid @RequestBody PointsAdjustmentRequest request,
            Authentication authentication
    ) {
        UUID adminUserId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(
                adminPointsService.adjustPoints(userId, request, adminUserId)
        );
    }
}
