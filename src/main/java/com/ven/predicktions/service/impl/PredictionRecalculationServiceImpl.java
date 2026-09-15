package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.match.MatchRecalculationResponse;
import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.Prediction;
import com.ven.predicktions.repository.PredictionRepository;
import com.ven.predicktions.service.PredictionRecalculationService;
import com.ven.predicktions.service.PredictionScoringService;
import com.ven.predicktions.service.AdminAuditService;
import com.ven.predicktions.model.AdminAuditAction;
import com.ven.predicktions.model.AdminAuditTargetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

@Service
public class PredictionRecalculationServiceImpl implements PredictionRecalculationService {

    private static final Logger log =
            LoggerFactory.getLogger(PredictionRecalculationServiceImpl.class);

    private final PredictionRepository predictionRepository;
    private final PredictionScoringService scoringService;
    private final AdminAuditService auditService;

    public PredictionRecalculationServiceImpl(
            PredictionRepository predictionRepository,
            PredictionScoringService scoringService
    ) {
        this(predictionRepository, scoringService, null);
    }
    @Autowired
    public PredictionRecalculationServiceImpl(PredictionRepository predictionRepository,
            PredictionScoringService scoringService, AdminAuditService auditService) {
        this.predictionRepository = predictionRepository;
        this.scoringService = scoringService;
        this.auditService = auditService;
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

        MatchRecalculationResponse response = new MatchRecalculationResponse(
                match.getId(),
                match.getHomeScore(),
                match.getAwayScore(),
                processed,
                updated,
                processed - updated,
                Instant.now()
        );
        if (auditService != null) {
            auditService.record(adminId, AdminAuditAction.PREDICTIONS_RECALCULATED,
                    AdminAuditTargetType.MATCH, match.getId(),
                    "Recalculated predictions: processed=" + processed + ", updated=" + updated +
                            ", unchanged=" + (processed - updated));
        }
        return response;
    }
}
