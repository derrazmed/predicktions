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
}