package com.ven.predicktions.dto.user;

import java.time.Instant;
import java.util.UUID;

public record AdjustmentHistoryFilter(
        UUID userId,
        UUID adminId,
        Instant from,
        Instant to
) {
}
