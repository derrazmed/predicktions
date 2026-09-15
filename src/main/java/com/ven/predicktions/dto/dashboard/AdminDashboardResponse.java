package com.ven.predicktions.dto.dashboard;

public record AdminDashboardResponse(
        long users,
        long activeUsers,
        long admins,
        long leagues,
        long predictions,
        long matches,
        long predictionsToday
) {
}
