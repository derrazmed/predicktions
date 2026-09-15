package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.match.MatchResponse;
import com.ven.predicktions.dto.match.UpdateMatchResultRequest;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.mapper.MatchMapper;
import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.MatchStatus;
import com.ven.predicktions.model.Prediction;
import com.ven.predicktions.model.AdminAuditAction;
import com.ven.predicktions.model.AdminAuditTargetType;
import com.ven.predicktions.service.AdminAuditService;
import com.ven.predicktions.repository.MatchRepository;
import com.ven.predicktions.service.AdminMatchService;
import com.ven.predicktions.service.PredictionRecalculationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Service
public class AdminMatchServiceImpl implements AdminMatchService {

    private static final Logger log = LoggerFactory.getLogger(AdminMatchServiceImpl.class);
    private final MatchRepository matchRepository;
    private final PredictionRecalculationService predictionRecalculationService;
    private final MatchMapper matchMapper;
    private final AdminAuditService auditService;

    public AdminMatchServiceImpl(
            MatchRepository matchRepository,
            PredictionRecalculationService predictionRecalculationService,
            MatchMapper matchMapper
    ) {
        this(matchRepository, predictionRecalculationService, matchMapper, null);
    }
    @Autowired
    public AdminMatchServiceImpl(MatchRepository matchRepository,
            PredictionRecalculationService predictionRecalculationService,
            MatchMapper matchMapper, AdminAuditService auditService) {
        this.matchRepository = matchRepository;
        this.predictionRecalculationService = predictionRecalculationService;
        this.matchMapper = matchMapper;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public MatchResponse updateResult(
            UUID matchId,
            UpdateMatchResultRequest request,
            UUID adminId
    ) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));

        Integer previousHomeScore = match.getHomeScore();
        Integer previousAwayScore = match.getAwayScore();
        boolean unchanged = java.util.Objects.equals(
                match.getHomeScore(), request.homeScore()
        ) && java.util.Objects.equals(
                match.getAwayScore(), request.awayScore()
        );

        if (!unchanged) {
            match.setHomeScore(request.homeScore());
            match.setAwayScore(request.awayScore());
            match.setStatus(MatchStatus.FINISHED);
            matchRepository.save(match);
            predictionRecalculationService.recalculate(match, adminId);
            if (auditService != null) {
                auditService.record(adminId, AdminAuditAction.MATCH_RESULT_CHANGED,
                        AdminAuditTargetType.MATCH, matchId,
                        "Changed result from " + previousHomeScore + "-" + previousAwayScore +
                                " to " + request.homeScore() + "-" + request.awayScore());
            }
        }

        log.info("Admin {} corrected match {} result from {}-{} to {}-{}",
                adminId, matchId, previousHomeScore, previousAwayScore,
                request.homeScore(), request.awayScore());
        return matchMapper.toResponse(match);
    }

    @Override
    @Transactional
    public com.ven.predicktions.dto.match.MatchRecalculationResponse recalculate(
            UUID matchId,
            UUID adminId
    ) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
        return predictionRecalculationService.recalculate(match, adminId);
    }
}
