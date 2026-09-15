package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.user.PointsAdjustmentRequest;
import com.ven.predicktions.dto.user.PointsAdjustmentResponse;
import com.ven.predicktions.dto.user.AdminPointsAdjustmentPageResponse;
import com.ven.predicktions.dto.user.AdminPointsAdjustmentResponse;
import com.ven.predicktions.dto.user.AdjustmentHistoryFilter;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.model.PointsAdjustment;
import com.ven.predicktions.model.User;
import com.ven.predicktions.model.AdminAuditAction;
import com.ven.predicktions.model.AdminAuditTargetType;
import com.ven.predicktions.repository.PointsAdjustmentRepository;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.AdminPointsService;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

@Service
public class AdminPointsServiceImpl implements AdminPointsService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final PointsAdjustmentRepository pointsAdjustmentRepository;
    private final com.ven.predicktions.service.AdminAuditService auditService;

    public AdminPointsServiceImpl(
            UserRepository userRepository,
            PointsAdjustmentRepository pointsAdjustmentRepository
    ) {
        this(userRepository, pointsAdjustmentRepository, null);
    }
    @Autowired
    public AdminPointsServiceImpl(UserRepository userRepository,
            PointsAdjustmentRepository pointsAdjustmentRepository,
            com.ven.predicktions.service.AdminAuditService auditService) {
        this.userRepository = userRepository;
        this.pointsAdjustmentRepository = pointsAdjustmentRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public PointsAdjustmentResponse adjustPoints(
            UUID userId,
            PointsAdjustmentRequest request,
            UUID adminUserId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        PointsAdjustment adjustment = pointsAdjustmentRepository.save(
                new PointsAdjustment(
                        user,
                        request.points(),
                        request.reason(),
                        admin
                )
        );
        if (auditService != null) {
            auditService.record(adminUserId, AdminAuditAction.POINTS_ADJUSTED,
                    AdminAuditTargetType.USER, userId,
                    "Adjusted points by " + (request.points() >= 0 ? "+" : "") +
                            request.points() + ": " + request.reason());
        }

        return new PointsAdjustmentResponse(
                user.getId(),
                user.getUsername(),
                adjustment.getPoints(),
                adjustment.getReason(),
                admin.getId(),
                adjustment.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPointsAdjustmentPageResponse getAdjustmentHistory(
            UUID userId,
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        return toPageResponse(pointsAdjustmentRepository.findAll(
                adjustmentSpecification(new AdjustmentHistoryFilter(
                        userId, null, null, null
                )),
                pageable(page, size)
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPointsAdjustmentPageResponse getAllAdjustmentHistory(
            AdjustmentHistoryFilter filter,
            int page,
            int size
    ) {
        validatePage(page, size);
        if (filter.from() != null && filter.to() != null
                && filter.from().isAfter(filter.to())) {
            throw new IllegalArgumentException("from must not be after to");
        }

        return toPageResponse(pointsAdjustmentRepository.findAll(
                adjustmentSpecification(filter),
                pageable(page, size)
        ));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }
    }

    private PageRequest pageable(int page, int size) {
        return PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );
    }

    private Specification<PointsAdjustment> adjustmentSpecification(
            AdjustmentHistoryFilter filter
    ) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (filter.userId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("user").get("id"), filter.userId()
                ));
            }
            if (filter.adminId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("adjustedBy").get("id"), filter.adminId()
                ));
            }
            if (filter.from() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"), filter.from()
                ));
            }
            if (filter.to() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("createdAt"), filter.to()
                ));
            }
            return criteriaBuilder.and(predicates.toArray(
                    jakarta.persistence.criteria.Predicate[]::new
            ));
        };
    }

    private AdminPointsAdjustmentPageResponse toPageResponse(
            Page<PointsAdjustment> adjustments
    ) {
        return new AdminPointsAdjustmentPageResponse(
                adjustments.getContent().stream()
                        .map(adjustment -> new AdminPointsAdjustmentResponse(
                                adjustment.getId(),
                                adjustment.getUser().getId(),
                                adjustment.getAdjustedBy().getId(),
                                adjustment.getPoints(),
                                adjustment.getReason(),
                                adjustment.getCreatedAt()
                        ))
                        .toList(),
                adjustments.getNumber(),
                adjustments.getSize(),
                adjustments.getTotalElements(),
                adjustments.getTotalPages()
        );
    }
}
