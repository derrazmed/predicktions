package com.ven.predicktions.service;

import com.ven.predicktions.model.League;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.LeagueMemberRepository;
import com.ven.predicktions.repository.LeagueRepository;
import com.ven.predicktions.service.impl.AdminLeagueDeletionServiceImpl;
import com.ven.predicktions.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminLeagueDeletionServiceTest {

    @Mock LeagueRepository leagueRepository;
    @Mock LeagueMemberRepository leagueMemberRepository;

    @Test
    void shouldDeleteOnlyMembershipsThenLeague() {
        UUID leagueId = UUID.randomUUID();
        League league = new League(
                "Abandoned",
                new User("owner", "owner@example.com", "hash"),
                "DELETE01"
        );
        UUID adminId = UUID.randomUUID();
        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(league));

        new AdminLeagueDeletionServiceImpl(
                leagueRepository, leagueMemberRepository
        ).deleteLeague(leagueId, adminId);

        verify(leagueMemberRepository).deleteAllByLeagueId(leagueId);
        verify(leagueRepository).delete(league);
        verifyNoMoreInteractions(leagueRepository, leagueMemberRepository);
    }

    @Test
    void shouldRejectUnknownLeagueWithoutDeletingAnything() {
        UUID leagueId = UUID.randomUUID();
        when(leagueRepository.findById(leagueId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new AdminLeagueDeletionServiceImpl(
                leagueRepository, leagueMemberRepository
        ).deleteLeague(leagueId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(leagueMemberRepository, never()).deleteAllByLeagueId(any());
        verify(leagueRepository, never()).delete(any());
    }
}
