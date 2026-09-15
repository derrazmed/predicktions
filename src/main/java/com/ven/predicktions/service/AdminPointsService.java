package com.ven.predicktions.service;

import com.ven.predicktions.dto.user.PointsAdjustmentRequest;
import com.ven.predicktions.dto.user.PointsAdjustmentResponse;
import com.ven.predicktions.dto.user.AdminPointsAdjustmentPageResponse;

import java.util.UUID;

public interface AdminPointsService {

    PointsAdjustmentResponse adjustPoints(
            UUID userId,
            PointsAdjustmentRequest request,
            UUID adminUserId
    );

    AdminPointsAdjustmentPageResponse getAdjustmentHistory(
            UUID userId,
            int page,
            int size
    );
}
