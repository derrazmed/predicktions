package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.user.PointsAdjustmentRequest;
import com.ven.predicktions.dto.user.PointsAdjustmentResponse;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.model.PointsAdjustment;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.PointsAdjustmentRepository;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.AdminPointsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminPointsServiceImpl implements AdminPointsService {

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
}
