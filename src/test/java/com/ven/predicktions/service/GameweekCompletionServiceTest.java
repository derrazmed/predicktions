package com.ven.predicktions.service;

import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.MatchStatus;
import com.ven.predicktions.model.Prediction;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.PredictionRepository;
import com.ven.predicktions.service.impl.PredictionScoringServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameweekCompletionServiceTest {

    @Mock
    private PredictionRepository predictionRepository;

    private GameweekCompletionService service;

    @BeforeEach
    void setUp() {
        service = new GameweekCompletionService(
                predictionRepository,
                new PredictionScoringServiceImpl()
        );
    }

    @Test
    void emptyGameweekIsNotComplete() {
        assertThat(service.isComplete(List.of())).isFalse();
        assertThat(service.isComplete(null)).isFalse();
        verifyNoInteractions(predictionRepository);
    }

    @Test
    void scheduledMatchMakesGameweekIncomplete() {
        Match match = match(MatchStatus.SCHEDULED, null, null);

        assertThat(service.isComplete(List.of(match))).isFalse();
        verifyNoInteractions(predictionRepository);
    }

    @Test
    void liveMatchMakesGameweekIncomplete() {
        Match match = match(MatchStatus.LIVE, 1, 0);

        assertThat(service.isComplete(List.of(match))).isFalse();
        verifyNoInteractions(predictionRepository);
    }

    @Test
    void finishedMatchWithoutResultMakesGameweekIncomplete() {
        Match match = match(MatchStatus.FINISHED, null, null);

        assertThat(service.isComplete(List.of(match))).isFalse();
        verifyNoInteractions(predictionRepository);
    }

    @Test
    void cancelledMatchIsConsideredTerminal() {
        Match match = match(MatchStatus.CANCELLED, null, null);

        when(predictionRepository.findAllByMatchIdIn(anyList()))
                .thenReturn(List.of());

        assertThat(service.isComplete(List.of(match))).isTrue();
    }

    @Test
    void gameweekIsCompleteWhenAllPredictionsAreScoredConsistently() {
        Match match = match(MatchStatus.FINISHED, 2, 1);
        Prediction exact = prediction(match, 2, 1);
        exact.setPoints(3);
        Prediction outcome = prediction(match, 3, 0);
        outcome.setPoints(1);

        when(predictionRepository.findAllByMatchIdIn(anyList()))
                .thenReturn(List.of(exact, outcome));

        assertThat(service.isComplete(List.of(match))).isTrue();
    }

    @Test
    void gameweekIsIncompleteWhenScoringDoesNotMatchResult() {
        Match match = match(MatchStatus.FINISHED, 2, 1);
        Prediction stale = prediction(match, 2, 1);
        stale.setPoints(0);

        when(predictionRepository.findAllByMatchIdIn(anyList()))
                .thenReturn(List.of(stale));

        assertThat(service.isComplete(List.of(match))).isFalse();
    }

    @Test
    void cancelledMatchesDoNotRequireScoring() {
        Match cancelled = match(MatchStatus.CANCELLED, null, null);
        Prediction prediction = prediction(cancelled, 2, 1);
        prediction.setPoints(0);

        when(predictionRepository.findAllByMatchIdIn(anyList()))
                .thenReturn(List.of(prediction));

        assertThat(service.isComplete(List.of(cancelled))).isTrue();
    }

    private Match match(MatchStatus status, Integer homeScore, Integer awayScore) {
        Match match = new Match(
                "match-" + UUID.randomUUID(),
                "Home FC",
                "Away FC",
                Instant.parse("2026-01-05T18:00:00Z"),
                status
        );
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);

        return match;
    }

    private Prediction prediction(Match match, int homeScore, int awayScore) {
        User user = new User(
                "user-" + UUID.randomUUID(),
                UUID.randomUUID() + "@example.com",
                "hash"
        );

        return new Prediction(user, match, homeScore, awayScore);
    }
}
