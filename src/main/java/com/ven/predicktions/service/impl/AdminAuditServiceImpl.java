package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.audit.*;
import com.ven.predicktions.model.*;
import com.ven.predicktions.repository.AdminAuditLogRepository;
import com.ven.predicktions.service.AdminAuditService;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class AdminAuditServiceImpl implements AdminAuditService {
    private static final int MAX_PAGE_SIZE = 100;
    private final AdminAuditLogRepository repository;
    public AdminAuditServiceImpl(AdminAuditLogRepository repository) { this.repository = repository; }

    @Override
    @Transactional
    public void record(UUID adminId, AdminAuditAction action, AdminAuditTargetType targetType,
                       UUID targetId, String details) {
        if (adminId == null || action == null || targetType == null || details == null || details.isBlank()) {
            throw new IllegalArgumentException("Audit data is required");
        }
        if (targetType == AdminAuditTargetType.LEADERBOARD && targetId != null) {
            throw new IllegalArgumentException("Leaderboard audit target must not have an id");
        }
        repository.save(new AdminAuditLog(adminId, action, targetType, targetId, details));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAuditLogPageResponse find(UUID adminId, AdminAuditAction action,
                                          AdminAuditTargetType targetType, UUID targetId,
                                          Instant from, Instant to, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        Specification<AdminAuditLog> spec = (root, query, cb) -> {
            var p = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (adminId != null) p.add(cb.equal(root.get("adminId"), adminId));
            if (action != null) p.add(cb.equal(root.get("action"), action));
            if (targetType != null) p.add(cb.equal(root.get("targetType"), targetType));
            if (targetId != null) p.add(cb.equal(root.get("targetId"), targetId));
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) p.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            return cb.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        Page<AdminAuditLog> result = repository.findAll(spec, PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return new AdminAuditLogPageResponse(result.getContent().stream().map(x ->
                new AdminAuditLogResponse(x.getId(), x.getAdminId(), x.getAction(), x.getTargetType(),
                        x.getTargetId(), x.getDetails(), x.getCreatedAt())).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
