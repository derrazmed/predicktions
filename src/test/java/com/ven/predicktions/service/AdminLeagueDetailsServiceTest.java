package com.ven.predicktions.service;

import com.ven.predicktions.dto.league.AdminLeagueDetailsResponse;
import com.ven.predicktions.model.League;
import com.ven.predicktions.model.LeagueMember;
import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.MatchStatus;
import com.ven.predicktions.model.Prediction;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.LeagueMemberRepository;
import com.ven.predicktions.repository.LeagueRepository;
import com.ven.predicktions.repository.LeaguePredictionStatistics;
import com.ven.predicktions.repository.PredictionRepository;
import com.ven.predicktions.service.impl.AdminLeagueDetailsServiceImpl;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminLeagueDetailsServiceTest {

    @Mock LeagueRepository leagueRepository;
    @Mock LeagueMemberRepository leagueMemberRepository;
    @Mock LeaderboardService leaderboardService;
    @Mock PredictionRepository predictionRepository;
    @Mock LeaguePredictionStatistics statistics;

    @Test
    void shouldMapLeagueMembersLeaderboardAndStatistics() {
        User owner = new User("owner", "owner@example.com", "hash");
        User member = new User("member", "member@example.com", "hash");
        League league = new League("League", owner, "JOIN123");
        league.addMember(member);
        UUID leagueId = UUID.randomUUID();
        org.springframework.test.util.ReflectionTestUtils.setField(league, "id", leagueId);

        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(league));
        when(leagueMemberRepository.findAllByLeagueIdOrdered(leagueId))
                .thenReturn(List.of(new LeagueMember(league, owner), new LeagueMember(league, member)));
        when(leaderboardService.getLeagueLeaderboardForAdmin(leagueId)).thenReturn(List.of());
        when(predictionRepository.findLeaguePredictionStatistics(leagueId)).thenReturn(statistics);
        when(statistics.getTotalPredictions()).thenReturn(4L);
        when(statistics.getPredictionsWithPoints()).thenReturn(3L);
        when(statistics.getTotalPointsAwarded()).thenReturn(7L);
        when(statistics.getExactScorePredictions()).thenReturn(1L);
        when(statistics.getParticipatingMembers()).thenReturn(2L);

        AdminLeagueDetailsResponse response =
                new AdminLeagueDetailsServiceImpl(
                        leagueRepository, leagueMemberRepository,
                        leaderboardService, predictionRepository
                ).getLeague(leagueId);

        assertThat(response.name()).isEqualTo("League");
        assertThat(response.memberCount()).isEqualTo(2);
        assertThat(response.owner().username()).isEqualTo("owner");
        assertThat(response.members()).hasSize(2);
        assertThat(response.statistics().totalPredictions()).isEqualTo(4);
        assertThat(response.statistics().averagePointsPerPrediction()).isEqualTo(1.75);
    }
}
