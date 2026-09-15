package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.match.MatchRecalculationResponse;
import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.Prediction;
import com.ven.predicktions.repository.PredictionRepository;
import com.ven.predicktions.service.PredictionRecalculationService;
import com.ven.predicktions.service.PredictionScoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PredictionRecalculationServiceImpl implements PredictionRecalculationService {

    private static final Logger log =
            LoggerFactory.getLogger(PredictionRecalculationServiceImpl.class);

    private final PredictionRepository predictionRepository;
    private final PredictionScoringService scoringService;

    public PredictionRecalculationServiceImpl(
            PredictionRepository predictionRepository,
            PredictionScoringService scoringService
    ) {
        this.predictionRepository = predictionRepository;
        this.scoringService = scoringService;
    }

    @Override
    @Transactional
    public MatchRecalculationResponse recalculate(Match match, UUID adminId) {
        int processed = 0;
        int updated = 0;

        for (Prediction prediction : predictionRepository.findAllByMatchId(match.getId())) {
            processed++;
            int calculatedPoints = scoringService.calculatePoints(prediction, match);
            if (prediction.getPoints() != calculatedPoints) {
                prediction.setPoints(calculatedPoints);
                predictionRepository.save(prediction);
                updated++;
            }
        }

        log.info("Admin {} recalculated match {} predictions: processed={}, updated={}",
                adminId, match.getId(), processed, updated);

        return new MatchRecalculationResponse(
                match.getId(),
                match.getHomeScore(),
                match.getAwayScore(),
                processed,
                updated,
                processed - updated,
                Instant.now()
        );
    }
}
