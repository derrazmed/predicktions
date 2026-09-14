package com.ven.predicktions.service;

import com.ven.predicktions.dto.prediction.AdminPredictionPageResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface AdminPredictionService {

    AdminPredictionPageResponse getPredictions(
            UUID userId,
            UUID matchId,
            Integer gameweek,
            UUID leagueId,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    );

    AdminPredictionPageResponse getUserPredictions(UUID userId, int page, int size);
}
