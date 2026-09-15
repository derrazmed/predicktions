package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.user.PointsAdjustmentRequest;
import com.ven.predicktions.dto.user.PointsAdjustmentResponse;
import com.ven.predicktions.dto.user.AdminPointsAdjustmentPageResponse;
import com.ven.predicktions.dto.user.AdminPointsAdjustmentResponse;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.model.PointsAdjustment;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.PointsAdjustmentRepository;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.AdminPointsService;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminPointsServiceImpl implements AdminPointsService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final PointsAdjustmentRepository pointsAdjustmentRepository;

    public AdminPointsServiceImpl(
            UserRepository userRepository,
            PointsAdjustmentRepository pointsAdjustmentRepository
    ) {
        this.userRepository = userRepository;
        this.pointsAdjustmentRepository = pointsAdjustmentRepository;
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

        Page<PointsAdjustment> adjustments =
                pointsAdjustmentRepository.findByUserIdOrderByCreatedAtDescIdDesc(
                        userId,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(
                                        Sort.Order.desc("createdAt"),
                                        Sort.Order.desc("id")
                                )
                        )
                );

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
