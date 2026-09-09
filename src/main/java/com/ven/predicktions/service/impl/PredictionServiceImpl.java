package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.prediction.CreatePredictionRequest;
import com.ven.predicktions.dto.prediction.PredictionResponse;
import com.ven.predicktions.exception.InvalidPredictionException;
import com.ven.predicktions.exception.PredictionLockedException;
import com.ven.predicktions.mapper.PredictionMapper;
import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.MatchStatus;
import com.ven.predicktions.model.Prediction;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.MatchRepository;
import com.ven.predicktions.repository.PredictionRepository;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.PredictionService;
import com.ven.predicktions.exception.DuplicateResourceException;
import com.ven.predicktions.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PredictionServiceImpl implements PredictionService {

    private final PredictionRepository predictionRepository;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final PredictionMapper predictionMapper;

    public PredictionServiceImpl(
            PredictionRepository predictionRepository,
            UserRepository userRepository,
            MatchRepository matchRepository,
            PredictionMapper predictionMapper
    ) {
        this.predictionRepository = predictionRepository;
        this.userRepository = userRepository;
        this.matchRepository = matchRepository;
        this.predictionMapper = predictionMapper;
    }

    @Override
    @Transactional
    public PredictionResponse createPrediction(
            UUID userId,
            CreatePredictionRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        Match match = matchRepository.findById(request.matchId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Match not found")
                );

        validatePredictionAllowed(match);

        if (predictionRepository
                .findByUserIdAndMatchId(userId, request.matchId())
                .isPresent()) {

            throw new DuplicateResourceException(
                    "Prediction already exists for this match"
            );
        }

        Prediction prediction = new Prediction(
                user,
                match,
                request.predictedHomeScore(),
                request.predictedAwayScore()
        );

        Prediction savedPrediction = predictionRepository.save(prediction);

        return predictionMapper.toResponse(savedPrediction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PredictionResponse> getUserPredictions(UUID userId) {
        return predictionRepository.findAllByUserId(userId)
                .stream()
                .map(predictionMapper::toResponse)
                .toList();
    }

    private void validatePredictionAllowed(Match match) {

        if (match.getStatus() == MatchStatus.FINISHED) {
            throw new InvalidPredictionException(
                    "Predictions cannot be submitted for a finished match."
            );
        }

        if (match.getStatus() == MatchStatus.CANCELLED) {
            throw new InvalidPredictionException(
                    "Predictions cannot be submitted for a cancelled match."
            );
        }

        if (!Instant.now().isBefore(match.getKickoffAt())) {
            throw new PredictionLockedException(
                    "Predictions are locked because the match has already started."
            );
        }
    }
}