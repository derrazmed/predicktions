package com.ven.predicktions.service;

import com.ven.predicktions.dto.dashboard.AdminDashboardResponse;
import com.ven.predicktions.model.Role;
import com.ven.predicktions.repository.LeagueRepository;
import com.ven.predicktions.repository.MatchRepository;
import com.ven.predicktions.repository.PredictionRepository;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.impl.AdminDashboardServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock UserRepository userRepository;
    @Mock LeagueRepository leagueRepository;
    @Mock PredictionRepository predictionRepository;
    @Mock MatchRepository matchRepository;

    @Test
    void shouldAggregateAllDashboardStatistics() {
        when(userRepository.count()).thenReturn(152L);
        when(userRepository.countByEnabledTrue()).thenReturn(121L);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(2L);
        when(leagueRepository.count()).thenReturn(34L);
        when(predictionRepository.count()).thenReturn(4821L);
        when(matchRepository.count()).thenReturn(120L);
        when(predictionRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                any(Instant.class), any(Instant.class)
        )).thenReturn(87L);

        AdminDashboardResponse response = new AdminDashboardServiceImpl(
                userRepository, leagueRepository, predictionRepository, matchRepository
        ).getDashboard();

        assertThat(response.users()).isEqualTo(152);
        assertThat(response.activeUsers()).isEqualTo(121);
        assertThat(response.admins()).isEqualTo(2);
        assertThat(response.leagues()).isEqualTo(34);
        assertThat(response.predictions()).isEqualTo(4821);
        assertThat(response.matches()).isEqualTo(120);
        assertThat(response.predictionsToday()).isEqualTo(87);
    }

    @Test
    void shouldUseAnExplicitUtcDayRangeForToday() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByEnabledTrue()).thenReturn(0L);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(leagueRepository.count()).thenReturn(0L);
        when(predictionRepository.count()).thenReturn(0L);
        when(matchRepository.count()).thenReturn(0L);
        when(predictionRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                any(Instant.class), any(Instant.class)
        )).thenReturn(0L);

        new AdminDashboardServiceImpl(
                userRepository, leagueRepository, predictionRepository, matchRepository
        ).getDashboard();

        org.mockito.ArgumentCaptor<Instant> start = org.mockito.ArgumentCaptor.forClass(Instant.class);
        org.mockito.ArgumentCaptor<Instant> end = org.mockito.ArgumentCaptor.forClass(Instant.class);
        org.mockito.Mockito.verify(predictionRepository).countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                start.capture(), end.capture()
        );
        assertThat(end.getValue().minusSeconds(86_400)).isEqualTo(start.getValue());
        assertThat(start.getValue().toString()).endsWith("T00:00:00Z");
    }
}
