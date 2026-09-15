package com.ven.predicktions.dto.audit;

import com.ven.predicktions.model.AdminAuditAction;
import com.ven.predicktions.model.AdminAuditTargetType;
import java.time.Instant;
import java.util.UUID;

public record AdminAuditLogResponse(UUID id, UUID adminId, AdminAuditAction action,
                                    AdminAuditTargetType targetType, UUID targetId,
                                    String details, Instant createdAt) {}
