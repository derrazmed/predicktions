package com.ven.predicktions.service;

import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.Prediction;

public interface PredictionScoringService {

    int calculatePoints(Prediction prediction, Match match);
}
