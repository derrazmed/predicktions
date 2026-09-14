package com.ven.predicktions.dto.prediction;

import java.util.List;

public record AdminPredictionPageResponse(
        List<AdminPredictionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
