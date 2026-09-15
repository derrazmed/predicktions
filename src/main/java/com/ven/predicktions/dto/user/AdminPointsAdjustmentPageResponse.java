package com.ven.predicktions.dto.user;

import java.util.List;

public record AdminPointsAdjustmentPageResponse(
        List<AdminPointsAdjustmentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
