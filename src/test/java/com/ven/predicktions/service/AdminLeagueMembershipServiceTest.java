package com.ven.predicktions.service;

import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.model.League;
import com.ven.predicktions.model.LeagueMember;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.LeagueMemberRepository;
import com.ven.predicktions.repository.LeagueRepository;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.impl.AdminLeagueMembershipServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminLeagueMembershipServiceTest {

    @Mock LeagueRepository leagueRepository;
    @Mock LeagueMemberRepository leagueMemberRepository;
    @Mock UserRepository userRepository;

    @Test
    void shouldRemoveOnlyTheRequestedMembership() {
        UUID leagueId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        User owner = new User("owner", "owner@example.com", "hash");
        User member = new User("member", "member@example.com", "hash");
        org.springframework.test.util.ReflectionTestUtils.setField(owner, "id", UUID.randomUUID());
        org.springframework.test.util.ReflectionTestUtils.setField(member, "id", userId);
        League league = new League("League", owner, "JOIN123");
        LeagueMember membership = new LeagueMember(league, member);

        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(league));
        when(userRepository.findById(userId)).thenReturn(Optional.of(member));
        when(leagueMemberRepository.findByLeagueIdAndUserId(leagueId, userId))
                .thenReturn(Optional.of(membership));

        new AdminLeagueMembershipServiceImpl(
                leagueRepository, leagueMemberRepository, userRepository
        ).removeMember(leagueId, userId, adminId);

        verify(leagueMemberRepository).delete(membership);
        verify(leagueRepository, never()).delete(any());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void shouldRejectOwnerWithoutDeletingMembership() {
        UUID leagueId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        User owner = new User("owner", "owner@example.com", "hash");
        org.springframework.test.util.ReflectionTestUtils.setField(owner, "id", ownerId);
        League league = new League("League", owner, "JOIN123");

        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(league));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> new AdminLeagueMembershipServiceImpl(
                leagueRepository, leagueMemberRepository, userRepository
        ).removeMember(leagueId, ownerId, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot remove the league owner.");

        verify(leagueMemberRepository, never()).delete(any());
    }

    @Test
    void shouldRejectUnknownLeagueUserAndMembership() {
        UUID leagueId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AdminLeagueMembershipService service =
                new AdminLeagueMembershipServiceImpl(
                        leagueRepository, leagueMemberRepository, userRepository
                );

        when(leagueRepository.findById(leagueId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.removeMember(leagueId, userId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);

        when(leagueRepository.findById(leagueId)).thenReturn(
                Optional.of(new League("League", new User("owner", "o@e.com", "hash"), "JOIN123"))
        );
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.removeMember(leagueId, userId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
