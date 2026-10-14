package com.ven.predicktions.service;

import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.MatchStatus;
import com.ven.predicktions.model.Prediction;
import com.ven.predicktions.repository.PredictionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Decides whether an individual match is ready to be revealed in a comparison.
 *
 * <p>A match is ready when its final result has been persisted and every stored
 * prediction for it has been scored consistently with the scoring rules. A
 * gameweek does not need to be finished: each match becomes visible on its own
 * as soon as it satisfies this rule.
 */
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

    /**
     * @return {@code true} when the match has a persisted final result and all
     *         of its predictions have been scored.
     */
    public boolean isMatchReady(Match match) {
        if (match == null || !hasFinalResult(match)) {
            return false;
        }

        return allPredictionsScored(List.of(match));
    }

    /**
     * Batch variant used by the comparison service: returns the ids of the
     * supplied matches that are ready to be revealed.
     */
    public Set<UUID> readyMatchIds(Collection<Match> matches) {
        if (matches == null || matches.isEmpty()) {
            return Set.of();
        }

        List<Match> candidates = matches.stream()
                .filter(this::hasFinalResult)
                .toList();

        if (candidates.isEmpty()) {
            return Set.of();
        }

        Map<UUID, Boolean> scoredByMatch = new HashMap<>();

        for (Prediction prediction : predictionRepository.findAllByMatchIdIn(idsOf(candidates))) {
            Match match = prediction.getMatch();

            if (match == null || !hasFinalResult(match)) {
                continue;
            }

            int expectedPoints = scoringService.calculatePoints(prediction, match);
            boolean scored = prediction.getPoints() != null
                    && prediction.getPoints() == expectedPoints;

            scoredByMatch.merge(match.getId(), scored, (current, next) -> current && next);
        }

        Set<UUID> ready = new HashSet<>();

        for (Match candidate : candidates) {
            if (scoredByMatch.getOrDefault(candidate.getId(), true)) {
                ready.add(candidate.getId());
            }
        }

        return ready;
    }

    private boolean allPredictionsScored(List<Match> matches) {
        for (Prediction prediction : predictionRepository.findAllByMatchIdIn(idsOf(matches))) {
            Match match = prediction.getMatch();

            if (match == null || !hasFinalResult(match)) {
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

    private boolean hasFinalResult(Match match) {
        return match.getStatus() == MatchStatus.FINISHED
                && match.getHomeScore() != null
                && match.getAwayScore() != null;
    }

    private List<UUID> idsOf(List<Match> matches) {
        return matches.stream()
                .map(Match::getId)
                .toList();
    }
}
