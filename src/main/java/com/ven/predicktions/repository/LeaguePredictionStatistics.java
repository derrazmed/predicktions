package com.ven.predicktions.repository;

public interface LeaguePredictionStatistics {

    long getTotalPredictions();

    long getPredictionsWithPoints();

    long getTotalPointsAwarded();

    long getExactScorePredictions();

    long getParticipatingMembers();
}
