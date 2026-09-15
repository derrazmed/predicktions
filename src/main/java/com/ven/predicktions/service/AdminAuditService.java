package com.ven.predicktions.service;

import com.ven.predicktions.dto.audit.*;
import com.ven.predicktions.model.*;
import java.time.Instant;
import java.util.UUID;

public interface AdminAuditService {
    void record(UUID adminId, AdminAuditAction action, AdminAuditTargetType targetType,
                UUID targetId, String details);
    AdminAuditLogPageResponse find(UUID adminId, AdminAuditAction action,
                                   AdminAuditTargetType targetType, UUID targetId,
                                   Instant from, Instant to, int page, int size);
}
