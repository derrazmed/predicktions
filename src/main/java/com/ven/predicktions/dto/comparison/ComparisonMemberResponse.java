package com.ven.predicktions.dto.comparison;

import java.util.UUID;

public record ComparisonMemberResponse(
        UUID userId,
        String username
) {
}
