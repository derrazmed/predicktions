package com.ven.predicktions.mapper;

import com.ven.predicktions.dto.prediction.PredictionResponse;
import com.ven.predicktions.model.Prediction;
import org.springframework.stereotype.Component;

@Component
public class PredictionMapper {

    public PredictionResponse toResponse(Prediction prediction) {
        return new PredictionResponse(
                prediction.getId(),
                prediction.getMatch().getId(),
                prediction.getPredictedHomeScore(),
                prediction.getPredictedAwayScore(),
                prediction.getPoints(),
                prediction.getCreatedAt(),
                prediction.getUpdatedAt()
        );
    }
}