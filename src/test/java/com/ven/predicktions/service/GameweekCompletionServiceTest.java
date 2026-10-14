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
import org.springframework.test.util.ReflectionTestUtils;

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
    void nullMatchAndEmptyCollectionAreNotReady() {
        assertThat(service.isMatchReady(null)).isFalse();
        assertThat(service.readyMatchIds(List.of())).isEmpty();
        assertThat(service.readyMatchIds(null)).isEmpty();
        verifyNoInteractions(predictionRepository);
    }

    @Test
    void scheduledMatchIsNotReady() {
        Match match = match(MatchStatus.SCHEDULED, null, null);

        assertThat(service.isMatchReady(match)).isFalse();
        verifyNoInteractions(predictionRepository);
    }

    @Test
    void liveMatchIsNotReady() {
        Match match = match(MatchStatus.LIVE, 1, 0);

        assertThat(service.isMatchReady(match)).isFalse();
        verifyNoInteractions(predictionRepository);
    }

    @Test
    void finishedMatchWithoutResultIsNotReady() {
        Match match = match(MatchStatus.FINISHED, null, null);

        assertThat(service.isMatchReady(match)).isFalse();
        verifyNoInteractions(predictionRepository);
    }

    @Test
    void cancelledMatchIsNotReadyBecauseItHasNoFinalResult() {
        Match match = match(MatchStatus.CANCELLED, null, null);

        assertThat(service.isMatchReady(match)).isFalse();
        verifyNoInteractions(predictionRepository);
    }

    @Test
    void finishedMatchIsReadyWhenAllPredictionsAreScoredConsistently() {
        Match match = match(MatchStatus.FINISHED, 2, 1);
        Prediction exact = prediction(match, 2, 1);
        exact.setPoints(3);
        Prediction outcome = prediction(match, 3, 0);
        outcome.setPoints(1);

        when(predictionRepository.findAllByMatchIdIn(anyList()))
                .thenReturn(List.of(exact, outcome));

        assertThat(service.isMatchReady(match)).isTrue();
    }

    @Test
    void finishedMatchIsNotReadyWhenScoringDoesNotMatchResult() {
        Match match = match(MatchStatus.FINISHED, 2, 1);
        Prediction stale = prediction(match, 2, 1);
        stale.setPoints(0);

        when(predictionRepository.findAllByMatchIdIn(anyList()))
                .thenReturn(List.of(stale));

        assertThat(service.isMatchReady(match)).isFalse();
    }

    @Test
    void finishedMatchWithoutPredictionsIsReady() {
        Match match = match(MatchStatus.FINISHED, 1, 0);

        when(predictionRepository.findAllByMatchIdIn(anyList()))
                .thenReturn(List.of());

        assertThat(service.isMatchReady(match)).isTrue();
    }

    @Test
    void readyMatchIdsReturnsOnlyFinishedAndScoredMatches() {
        Match ready = persistedMatch(MatchStatus.FINISHED, 2, 1);
        Match stale = persistedMatch(MatchStatus.FINISHED, 2, 1);
        Match scheduled = persistedMatch(MatchStatus.SCHEDULED, null, null);

        Prediction readyPrediction = prediction(ready, 2, 1);
        readyPrediction.setPoints(3);
        Prediction stalePrediction = prediction(stale, 2, 1);
        stalePrediction.setPoints(0);

        when(predictionRepository.findAllByMatchIdIn(anyList()))
                .thenReturn(List.of(readyPrediction, stalePrediction));

        assertThat(service.readyMatchIds(List.of(ready, stale, scheduled)))
                .containsExactly(ready.getId());
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

    private Match persistedMatch(MatchStatus status, Integer homeScore, Integer awayScore) {
        Match match = match(status, homeScore, awayScore);
        ReflectionTestUtils.setField(match, "id", UUID.randomUUID());

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
