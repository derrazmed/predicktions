package com.ven.predicktions.service.impl;

import com.ven.predicktions.integration.sports.SportsMatch;
import com.ven.predicktions.integration.sports.SportsProvider;
import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.MatchStatus;
import com.ven.predicktions.repository.MatchRepository;
import com.ven.predicktions.service.PredictionRecalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * A failure while scoring synchronised results must not be swallowed: the
 * synchronisation has to fail loudly instead of reporting an entirely
 * successful run.
 */
@ExtendWith(MockitoExtension.class)
class MatchSyncScoringFailureTest {

    @Mock
    private SportsProvider sportsProvider;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private PredictionRecalculationService predictionRecalculationService;

    private MatchSyncServiceImpl matchSyncService;

    @BeforeEach
    void setUp() {
        matchSyncService = new MatchSyncServiceImpl(
                sportsProvider,
                matchRepository,
                predictionRecalculationService
        );
    }

    @Test
    void scoringFailureIsPropagatedInsteadOfReportingSuccess() {
        SportsMatch finished = new SportsMatch(
                "575500",
                "Home",
                "Away",
                Instant.parse("2026-09-11T19:00:00Z"),
                2,
                1,
                MatchStatus.FINISHED
        );

        Match existing = new Match(
                "575500",
                "Home",
                "Away",
                Instant.parse("2026-09-11T19:00:00Z"),
                MatchStatus.SCHEDULED
        );

        when(sportsProvider.getMatchdayMatches("CL", 5))
                .thenReturn(List.of(finished));

        when(matchRepository.findByExternalId("575500"))
                .thenReturn(Optional.of(existing));

        when(matchRepository.save(any(Match.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new IllegalStateException("scoring failed"))
                .when(predictionRecalculationService)
                .recalculate(existing, null);

        assertThatThrownBy(() -> matchSyncService.synchronizeResults("CL", 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("scoring failed");
    }
}
