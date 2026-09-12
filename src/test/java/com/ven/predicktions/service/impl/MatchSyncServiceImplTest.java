package com.ven.predicktions.service.impl;

import com.ven.predicktions.integration.sports.SportsMatch;
import com.ven.predicktions.integration.sports.SportsProvider;
import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.MatchStatus;
import com.ven.predicktions.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchSyncServiceImplTest {

    @Mock
    private SportsProvider sportsProvider;

    @Mock
    private MatchRepository matchRepository;

    private MatchSyncServiceImpl matchSyncService;

    @BeforeEach
    void setUp() {
        matchSyncService = new MatchSyncServiceImpl(
                sportsProvider,
                matchRepository
        );
    }

    @Test
    void shouldCreateNewMatchesWhenTheyDoNotExist() {

        SportsMatch sportsMatch = new SportsMatch(
                "575335",
                "Fenerbahçe SK",
                "AS Roma",
                Instant.parse("2026-09-10T16:45:00Z"),
                null,
                null,
                MatchStatus.SCHEDULED
        );

        when(sportsProvider.getCompetitionMatches("CL"))
                .thenReturn(List.of(sportsMatch));

        when(matchRepository.findByExternalId("575335"))
                .thenReturn(Optional.empty());

        when(matchRepository.save(any(Match.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        matchSyncService.synchronizeFixtures("CL");

        ArgumentCaptor<Match> captor =
                ArgumentCaptor.forClass(Match.class);

        verify(matchRepository).save(captor.capture());

        Match savedMatch = captor.getValue();

        assertThat(savedMatch.getExternalId())
                .isEqualTo("575335");

        assertThat(savedMatch.getHomeTeam())
                .isEqualTo("Fenerbahçe SK");

        assertThat(savedMatch.getAwayTeam())
                .isEqualTo("AS Roma");

        assertThat(savedMatch.getKickoffAt())
                .isEqualTo(
                        Instant.parse("2026-09-10T16:45:00Z")
                );

        assertThat(savedMatch.getHomeScore())
                .isNull();

        assertThat(savedMatch.getAwayScore())
                .isNull();

        assertThat(savedMatch.getStatus())
                .isEqualTo(MatchStatus.SCHEDULED);
    }

    @Test
    void shouldUpdateExistingMatchInsteadOfCreatingDuplicate() {

        Match existingMatch = new Match(
                "575335",
                "Fenerbahçe SK",
                "AS Roma",
                Instant.parse("2026-09-10T16:45:00Z"),
                MatchStatus.SCHEDULED
        );

        SportsMatch finishedMatch = new SportsMatch(
                "575335",
                "Fenerbahçe SK",
                "AS Roma",
                Instant.parse("2026-09-10T16:45:00Z"),
                2,
                1,
                MatchStatus.FINISHED
        );

        when(sportsProvider.getMatchdayMatches("CL", 1))
                .thenReturn(List.of(finishedMatch));

        when(matchRepository.findByExternalId("575335"))
                .thenReturn(Optional.of(existingMatch));

        when(matchRepository.save(any(Match.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        matchSyncService.synchronizeResults("CL", 1);

        verify(matchRepository).save(existingMatch);

        assertThat(existingMatch.getExternalId())
                .isEqualTo("575335");

        assertThat(existingMatch.getHomeScore())
                .isEqualTo(2);

        assertThat(existingMatch.getAwayScore())
                .isEqualTo(1);

        assertThat(existingMatch.getStatus())
                .isEqualTo(MatchStatus.FINISHED);
    }

    @Test
    void shouldSynchronizeMultipleMatchesInOneBatch() {

        SportsMatch firstMatch = new SportsMatch(
                "575335",
                "Fenerbahçe SK",
                "AS Roma",
                Instant.parse("2026-09-10T16:45:00Z"),
                null,
                null,
                MatchStatus.SCHEDULED
        );

        SportsMatch secondMatch = new SportsMatch(
                "575336",
                "PSV",
                "Shakhtar Donetsk",
                Instant.parse("2026-09-10T19:00:00Z"),
                null,
                null,
                MatchStatus.SCHEDULED
        );

        when(sportsProvider.getCompetitionMatches("CL"))
                .thenReturn(
                        List.of(firstMatch, secondMatch)
                );

        when(matchRepository.findByExternalId("575335"))
                .thenReturn(Optional.empty());

        when(matchRepository.findByExternalId("575336"))
                .thenReturn(Optional.empty());

        when(matchRepository.save(any(Match.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        matchSyncService.synchronizeFixtures("CL");

        verify(sportsProvider)
                .getCompetitionMatches("CL");

        verify(matchRepository)
                .findByExternalId("575335");

        verify(matchRepository)
                .findByExternalId("575336");

        verify(matchRepository, times(2))
                .save(any(Match.class));
    }

    @Test
    void shouldNotSaveAnythingWhenProviderReturnsNoMatches() {

        when(sportsProvider.getCompetitionMatches("CL"))
                .thenReturn(List.of());

        matchSyncService.synchronizeFixtures("CL");

        verify(sportsProvider)
                .getCompetitionMatches("CL");

        verify(matchRepository, never())
                .findByExternalId(anyString());

        verify(matchRepository, never())
                .save(any(Match.class));
    }

    @Test
    void shouldPropagateProviderFailure() {

        RuntimeException exception =
                new RuntimeException(
                        "External provider unavailable"
                );

        when(sportsProvider.getCompetitionMatches("CL"))
                .thenThrow(exception);

        RuntimeException thrown =
                org.assertj.core.api.Assertions.catchThrowableOfType(
                        () ->
                                matchSyncService
                                        .synchronizeFixtures("CL"),
                        RuntimeException.class
                );

        assertThat(thrown)
                .isSameAs(exception);

        verify(matchRepository, never())
                .findByExternalId(anyString());

        verify(matchRepository, never())
                .save(any(Match.class));
    }

    @Test
    void shouldSynchronizeResultsUsingMatchday() {

        SportsMatch finishedMatch = new SportsMatch(
                "575337",
                "Barcelona",
                "Bayern München",
                Instant.parse("2026-09-11T19:00:00Z"),
                3,
                2,
                MatchStatus.FINISHED
        );

        when(sportsProvider.getMatchdayMatches("CL", 2))
                .thenReturn(List.of(finishedMatch));

        Match existingMatch = new Match(
                "575337",
                "Barcelona",
                "Bayern München",
                Instant.parse("2026-09-11T19:00:00Z"),
                MatchStatus.SCHEDULED
        );

        when(matchRepository.findByExternalId("575337"))
                .thenReturn(Optional.of(existingMatch));

        when(matchRepository.save(any(Match.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        matchSyncService.synchronizeResults("CL", 2);

        verify(sportsProvider)
                .getMatchdayMatches("CL", 2);

        verify(matchRepository)
                .findByExternalId("575337");

        verify(matchRepository)
                .save(existingMatch);

        assertThat(existingMatch.getHomeScore())
                .isEqualTo(3);

        assertThat(existingMatch.getAwayScore())
                .isEqualTo(2);

        assertThat(existingMatch.getStatus())
                .isEqualTo(MatchStatus.FINISHED);
    }
}