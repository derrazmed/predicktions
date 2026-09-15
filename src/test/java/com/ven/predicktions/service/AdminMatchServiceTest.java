package com.ven.predicktions.service;

import com.ven.predicktions.dto.match.UpdateMatchResultRequest;
import com.ven.predicktions.mapper.MatchMapper;
import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.MatchStatus;
import com.ven.predicktions.model.Prediction;
import com.ven.predicktions.repository.MatchRepository;
import com.ven.predicktions.repository.PredictionRepository;
import com.ven.predicktions.service.impl.AdminMatchServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminMatchServiceTest {

    @Mock MatchRepository matchRepository;
    @Mock PredictionRepository predictionRepository;
    @Mock MatchMapper matchMapper;

    @Test
    void shouldUpdateResultAndRecalculateAllPredictions() {
        Match match = new Match("external", "Home", "Away",
                Instant.now(), MatchStatus.LIVE);
        Prediction exact = new Prediction(
                new com.ven.predicktions.model.User("a", "a@example.com", "hash"),
                match, 2, 1);
        Prediction outcome = new Prediction(
                new com.ven.predicktions.model.User("b", "b@example.com", "hash"),
                match, 3, 0);
        when(matchRepository.findById(any())).thenReturn(Optional.of(match));
        when(predictionRepository.findAllByMatchId(any()))
                .thenReturn(List.of(exact, outcome));

        new AdminMatchServiceImpl(
                matchRepository, predictionRepository, matchMapper
        ).updateResult(
                UUID.randomUUID(),
                new UpdateMatchResultRequest(2, 1),
                UUID.randomUUID()
        );

        assertThat(match.getHomeScore()).isEqualTo(2);
        assertThat(match.getAwayScore()).isEqualTo(1);
        assertThat(match.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(exact.getPoints()).isEqualTo(3);
        assertThat(outcome.getPoints()).isEqualTo(1);
        verify(predictionRepository, times(2)).save(any(Prediction.class));
        verify(matchRepository).save(match);
    }
}
