package com.ven.predicktions.service;

import com.ven.predicktions.dto.prediction.CreatePredictionRequest;
import com.ven.predicktions.dto.prediction.PredictionResponse;

import java.util.List;
import java.util.UUID;

public interface PredictionService {

    PredictionResponse createPrediction(UUID userId, CreatePredictionRequest request);
    List<PredictionResponse> getUserPredictions(UUID userId);
}