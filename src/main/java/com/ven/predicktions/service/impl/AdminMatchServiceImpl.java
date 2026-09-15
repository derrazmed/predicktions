package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.match.MatchResponse;
import com.ven.predicktions.dto.match.UpdateMatchResultRequest;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.mapper.MatchMapper;
import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.MatchStatus;
import com.ven.predicktions.model.Prediction;
import com.ven.predicktions.repository.MatchRepository;
import com.ven.predicktions.repository.PredictionRepository;
import com.ven.predicktions.service.AdminMatchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Service
public class AdminMatchServiceImpl implements AdminMatchService {

    private static final Logger log = LoggerFactory.getLogger(AdminMatchServiceImpl.class);
    private final MatchRepository matchRepository;
    private final PredictionRepository predictionRepository;
    private final MatchMapper matchMapper;

    public AdminMatchServiceImpl(
            MatchRepository matchRepository,
            PredictionRepository predictionRepository,
            MatchMapper matchMapper
    ) {
        this.matchRepository = matchRepository;
        this.predictionRepository = predictionRepository;
        this.matchMapper = matchMapper;
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
            for (Prediction prediction : predictionRepository.findAllByMatchId(matchId)) {
                prediction.setPoints(calculatePoints(
                        prediction.getPredictedHomeScore(),
                        prediction.getPredictedAwayScore(),
                        request.homeScore(),
                        request.awayScore()
                ));
                predictionRepository.save(prediction);
            }
            matchRepository.save(match);
        }

        log.info("Admin {} corrected match {} result from {}-{} to {}-{}",
                adminId, matchId, previousHomeScore, previousAwayScore,
                request.homeScore(), request.awayScore());
        return matchMapper.toResponse(match);
    }

    private int calculatePoints(
            int predictedHome,
            int predictedAway,
            int actualHome,
            int actualAway
    ) {
        if (predictedHome == actualHome && predictedAway == actualAway) {
            return 3;
        }
        if (Integer.signum(predictedHome - predictedAway)
                == Integer.signum(actualHome - actualAway)) {
            return 1;
        }
        return 0;
    }
}
