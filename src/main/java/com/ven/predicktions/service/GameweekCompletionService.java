package com.ven.predicktions.service;

import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.MatchStatus;
import com.ven.predicktions.model.Prediction;
import com.ven.predicktions.repository.PredictionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GameweekCompletionService {

    private final PredictionRepository predictionRepository;
    private final PredictionScoringService scoringService;

    public GameweekCompletionService(
            PredictionRepository predictionRepository,
            PredictionScoringService scoringService
    ) {
        this.predictionRepository = predictionRepository;
        this.scoringService = scoringService;
    }

    public boolean isComplete(Collection<Match> matches) {
        if (matches == null || matches.isEmpty()) {
            return false;
        }

        if (!allMatchesTerminal(matches)) {
            return false;
        }

        return allPredictionsScored(matches);
    }

    private boolean allMatchesTerminal(Collection<Match> matches) {
        for (Match match : matches) {
            if (isCancelled(match)) {
                continue;
            }

            if (match.getStatus() != MatchStatus.FINISHED
                    || match.getHomeScore() == null
                    || match.getAwayScore() == null) {
                return false;
            }
        }

        return true;
    }

    private boolean allPredictionsScored(Collection<Match> matches) {
        List<UUID> matchIds = matches.stream()
                .map(Match::getId)
                .toList();

        for (Prediction prediction : predictionRepository.findAllByMatchIdIn(matchIds)) {
            Match match = prediction.getMatch();

            if (match == null || isCancelled(match)) {
                continue;
            }

            int expectedPoints = scoringService.calculatePoints(prediction, match);

            if (prediction.getPoints() == null
                    || prediction.getPoints() != expectedPoints) {
                return false;
            }
        }

        return true;
    }

    private boolean isCancelled(Match match) {
        return match.getStatus() == MatchStatus.CANCELLED;
    }
}
