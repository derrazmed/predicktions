package com.ven.predicktions.controller;

import com.ven.predicktions.dto.audit.*;
import com.ven.predicktions.model.*;
import com.ven.predicktions.service.AdminAuditService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AdminAuditController {
    private final AdminAuditService service;
    public AdminAuditController(AdminAuditService service) { this.service = service; }
    @GetMapping
    public ResponseEntity<AdminAuditLogPageResponse> find(
            @RequestParam(required = false) UUID adminId,
            @RequestParam(required = false) AdminAuditAction action,
            @RequestParam(required = false) AdminAuditTargetType targetType,
            @RequestParam(required = false) UUID targetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.find(adminId, action, targetType, targetId, from, to, page, size));
    }
}
