package com.ven.predicktions.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_audit_logs", indexes = {
        @Index(name = "idx_admin_audit_logs_created_at", columnList = "created_at"),
        @Index(name = "idx_admin_audit_logs_admin_created", columnList = "admin_id, created_at"),
        @Index(name = "idx_admin_audit_logs_target_created", columnList = "target_type, target_id, created_at"),
        @Index(name = "idx_admin_audit_logs_action_created", columnList = "action, created_at")
})
public class AdminAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "admin_id", nullable = false)
    private UUID adminId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AdminAuditAction action;
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private AdminAuditTargetType targetType;
    @Column(name = "target_id")
    private UUID targetId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String details;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AdminAuditLog() {}
    public AdminAuditLog(UUID adminId, AdminAuditAction action, AdminAuditTargetType targetType,
                         UUID targetId, String details) {
        this.adminId = adminId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
    }
    @PrePersist protected void onCreate() { createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getAdminId() { return adminId; }
    public AdminAuditAction getAction() { return action; }
    public AdminAuditTargetType getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
