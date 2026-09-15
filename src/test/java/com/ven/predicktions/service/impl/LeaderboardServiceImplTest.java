package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.leaderboard.LeaderboardEntryResponse;
import com.ven.predicktions.repository.LeagueMemberRepository;
import com.ven.predicktions.repository.LeagueRepository;
import com.ven.predicktions.repository.PredictionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceImplTest {

    @Mock
    private PredictionRepository predictionRepository;

    @Mock
    private LeagueRepository leagueRepository;

    @Mock
    private LeagueMemberRepository leagueMemberRepository;

    private LeaderboardServiceImpl leaderboardService;

    @BeforeEach
    void setUp() {
        leaderboardService =
                new LeaderboardServiceImpl(predictionRepository, leagueRepository, leagueMemberRepository);
    }

    @Test
    void shouldRankUsersByTotalPointsDescending() {

        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        UUID charlieId = UUID.randomUUID();

        when(predictionRepository.findGlobalLeaderboard())
                .thenReturn(List.of(
                        new Object[]{aliceId, "alice", 100L},
                        new Object[]{bobId, "bob", 80L},
                        new Object[]{charlieId, "charlie", 50L}
                ));

        List<LeaderboardEntryResponse> result =
                leaderboardService.getGlobalLeaderboard();

        assertEquals(3, result.size());

        assertEquals(1, result.get(0).rank());
        assertEquals("alice", result.get(0).username());
        assertEquals(100L, result.get(0).totalPoints());

        assertEquals(2, result.get(1).rank());
        assertEquals("bob", result.get(1).username());
        assertEquals(80L, result.get(1).totalPoints());

        assertEquals(3, result.get(2).rank());
        assertEquals("charlie", result.get(2).username());
        assertEquals(50L, result.get(2).totalPoints());
    }

    @Test
    void shouldGiveSameRankToUsersWithEqualPoints() {

        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        UUID charlieId = UUID.randomUUID();

        when(predictionRepository.findGlobalLeaderboard())
                .thenReturn(List.of(
                        new Object[]{aliceId, "alice", 100L},
                        new Object[]{bobId, "bob", 80L},
                        new Object[]{charlieId, "charlie", 80L}
                ));

        List<LeaderboardEntryResponse> result =
                leaderboardService.getGlobalLeaderboard();

        assertEquals(1, result.get(0).rank());
        assertEquals(2, result.get(1).rank());
        assertEquals(2, result.get(2).rank());
    }

    @Test
    void shouldSkipRankAfterTie() {

        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        UUID charlieId = UUID.randomUUID();
        UUID davidId = UUID.randomUUID();

        when(predictionRepository.findGlobalLeaderboard())
                .thenReturn(List.of(
                        new Object[]{aliceId, "alice", 100L},
                        new Object[]{bobId, "bob", 80L},
                        new Object[]{charlieId, "charlie", 80L},
                        new Object[]{davidId, "david", 50L}
                ));

        List<LeaderboardEntryResponse> result =
                leaderboardService.getGlobalLeaderboard();

        assertEquals(1, result.get(0).rank());
        assertEquals(2, result.get(1).rank());
        assertEquals(2, result.get(2).rank());
        assertEquals(4, result.get(3).rank());
    }

    @Test
    void shouldReturnEmptyLeaderboardWhenThereAreNoPredictions() {

        when(predictionRepository.findGlobalLeaderboard())
                .thenReturn(List.of());

        List<LeaderboardEntryResponse> result =
                leaderboardService.getGlobalLeaderboard();

        assertTrue(result.isEmpty());

        verify(predictionRepository, times(1))
                .findGlobalLeaderboard();
    }

    @Test
    void shouldUseFilteredRepositoryQueryForGameweekAndSeason() {
        when(predictionRepository.findGlobalLeaderboard(2026, 3))
                .thenReturn(List.of());

        assertTrue(leaderboardService.getGlobalLeaderboard(3, 2026).isEmpty());
        verify(predictionRepository).findGlobalLeaderboard(2026, 3);
    }

    @Test
    void shouldRejectInvalidLeaderboardFilters() {
        assertThrows(IllegalArgumentException.class,
                () -> leaderboardService.getGlobalLeaderboard(0, null));
        assertThrows(IllegalArgumentException.class,
                () -> leaderboardService.getGlobalLeaderboard(54, null));
        assertThrows(IllegalArgumentException.class,
                () -> leaderboardService.getGlobalLeaderboard(null, 0));
    }

    @Test
    void shouldRecalculateFromExistingGlobalLeaderboardWithoutAccumulatingPoints() {
        when(predictionRepository.findGlobalLeaderboard())
                .thenReturn(List.of(
                        new Object[]{UUID.randomUUID(), "alice", 100L},
                        new Object[]{UUID.randomUUID(), "bob", 80L}
                ));

        var first = leaderboardService.recalculateGlobalLeaderboard(UUID.randomUUID());
        var second = leaderboardService.recalculateGlobalLeaderboard(UUID.randomUUID());

        assertEquals(2, first.usersProcessed());
        assertEquals(0, first.entriesUpdated());
        assertEquals(2, first.entriesUnchanged());
        assertEquals(2, second.usersProcessed());
        assertEquals(0, second.entriesUpdated());
        verify(predictionRepository, times(2)).findGlobalLeaderboard();
    }

    @Test
    void shouldRankNegativeAdjustedTotalsUsingExistingOrdering() {
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();

        when(predictionRepository.findGlobalLeaderboard())
                .thenReturn(List.of(
                        new Object[]{bobId, "bob", 95L},
                        new Object[]{aliceId, "alice", 90L}
                ));

        List<LeaderboardEntryResponse> result =
                leaderboardService.getGlobalLeaderboard();

        assertEquals("bob", result.get(0).username());
        assertEquals(95L, result.get(0).totalPoints());
        assertEquals("alice", result.get(1).username());
        assertEquals(90L, result.get(1).totalPoints());
    }
}