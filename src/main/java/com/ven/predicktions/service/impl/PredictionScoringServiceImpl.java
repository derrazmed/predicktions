package com.ven.predicktions.service.impl;

import com.ven.predicktions.exception.InvalidPredictionException;
import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.Prediction;
import com.ven.predicktions.service.PredictionScoringService;
import org.springframework.stereotype.Service;

@Service
public class PredictionScoringServiceImpl implements PredictionScoringService {

    @Override
    public int calculatePoints(Prediction prediction, Match match) {
        if (match.getHomeScore() == null || match.getAwayScore() == null) {
            throw new InvalidPredictionException(
                    "Prediction points cannot be recalculated before the match has a result"
            );
        }

        if (prediction.getPredictedHomeScore().equals(match.getHomeScore())
                && prediction.getPredictedAwayScore().equals(match.getAwayScore())) {
            return 3;
        }

        if (Integer.signum(
                prediction.getPredictedHomeScore() - prediction.getPredictedAwayScore()
        ) == Integer.signum(match.getHomeScore() - match.getAwayScore())) {
            return 1;
        }

        return 0;
    }
}
