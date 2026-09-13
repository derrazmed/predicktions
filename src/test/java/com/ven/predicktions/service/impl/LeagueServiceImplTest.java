package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.league.CreateLeagueRequest;
import com.ven.predicktions.dto.league.JoinLeagueRequest;
import com.ven.predicktions.dto.league.LeagueResponse;
import com.ven.predicktions.dto.league.UpdateLeagueRequest;
import com.ven.predicktions.exception.DuplicateResourceException;
import com.ven.predicktions.exception.ForbiddenOperationException;
import com.ven.predicktions.exception.OwnerCannotLeaveException;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.mapper.LeagueMapper;
import com.ven.predicktions.model.League;
import com.ven.predicktions.model.LeagueMember;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.LeagueMemberRepository;
import com.ven.predicktions.repository.LeagueRepository;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.JoinCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeagueServiceImplTest {

    @Mock
    private LeagueRepository leagueRepository;

    @Mock
    private LeagueMemberRepository leagueMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JoinCodeGenerator joinCodeGenerator;

    @Mock
    private LeagueMapper leagueMapper;

    private LeagueServiceImpl leagueService;

    private UUID userId;
    private User owner;

    @BeforeEach
    void setUp() {
        leagueService = new LeagueServiceImpl(
                leagueRepository,
                leagueMemberRepository,
                userRepository,
                joinCodeGenerator,
                leagueMapper
        );

        userId = UUID.randomUUID();
        owner = new User("owner", "owner@example.com", "hashed-password");
        ReflectionTestUtils.setField(owner, "id", userId);
    }

    @Test
    void createLeague_assignsOwnerAndGeneratedJoinCode() {
        CreateLeagueRequest request = new CreateLeagueRequest("Office League");
        LeagueResponse expectedResponse = new LeagueResponse(
                UUID.randomUUID(),
                "Office League",
                userId,
                1,
                List.of("owner"),
                "AB23KLP9",
                Instant.parse("2026-09-12T20:00:00Z")
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(joinCodeGenerator.generate()).thenReturn("AB23KLP9");
        when(leagueRepository.existsByJoinCode("AB23KLP9")).thenReturn(false);
        when(leagueRepository.save(any(League.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(leagueMemberRepository.findAllByLeagueId(null)).thenReturn(List.of());
        when(leagueMapper.toResponse(any(League.class), org.mockito.ArgumentMatchers.eq(0L), org.mockito.ArgumentMatchers.eq(List.of())))
                .thenReturn(expectedResponse);

        LeagueResponse response = leagueService.createLeague(userId, request);

        assertThat(response).isEqualTo(expectedResponse);

        ArgumentCaptor<League> leagueCaptor = ArgumentCaptor.forClass(League.class);
        verify(leagueRepository).save(leagueCaptor.capture());

        League savedLeague = leagueCaptor.getValue();
        assertThat(savedLeague.getName()).isEqualTo("Office League");
        assertThat(savedLeague.getOwner()).isEqualTo(owner);
        assertThat(savedLeague.getJoinCode()).isEqualTo("AB23KLP9");
        assertThat(savedLeague.getMembers()).hasSize(1);
        assertThat(savedLeague.getMembers().getFirst().getUser()).isEqualTo(owner);
    }

    @Test
    void createLeague_retriesWhenJoinCodeAlreadyExists() {
        CreateLeagueRequest request = new CreateLeagueRequest("Retry League");
        LeagueResponse expectedResponse = new LeagueResponse(
                UUID.randomUUID(),
                "Retry League",
                userId,
                1,
                List.of("owner"),
                "FREECODE",
                Instant.parse("2026-09-12T20:00:00Z")
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(joinCodeGenerator.generate()).thenReturn("TAKEN123", "FREECODE");
        when(leagueRepository.existsByJoinCode("TAKEN123")).thenReturn(true);
        when(leagueRepository.existsByJoinCode("FREECODE")).thenReturn(false);
        when(leagueRepository.save(any(League.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(leagueMemberRepository.findAllByLeagueId(null)).thenReturn(List.of());
        when(leagueMapper.toResponse(any(League.class), org.mockito.ArgumentMatchers.eq(0L), org.mockito.ArgumentMatchers.eq(List.of())))
                .thenReturn(expectedResponse);

        leagueService.createLeague(userId, request);

        ArgumentCaptor<League> leagueCaptor = ArgumentCaptor.forClass(League.class);
        verify(leagueRepository).save(leagueCaptor.capture());
        assertThat(leagueCaptor.getValue().getJoinCode()).isEqualTo("FREECODE");
    }

    @Test
    void createLeague_doesNotSaveWhenJoinCodeCannotBeGenerated() {
        CreateLeagueRequest request = new CreateLeagueRequest("Failed League");

        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(joinCodeGenerator.generate()).thenReturn("TAKEN123");
        when(leagueRepository.existsByJoinCode("TAKEN123")).thenReturn(true);

        assertThatThrownBy(() -> leagueService.createLeague(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to generate a unique join code");

        verify(leagueRepository, never()).save(any(League.class));
    }

    @Test
    void createLeague_throwsWhenUserDoesNotExist() {
        CreateLeagueRequest request = new CreateLeagueRequest("Missing Owner");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leagueService.createLeague(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verify(leagueRepository, never()).save(any(League.class));
        verify(joinCodeGenerator, never()).generate();
    }

    @Test
    void joinLeague_addsAuthenticatedUserAsMember() {
        UUID memberId = UUID.randomUUID();
        UUID leagueId = UUID.randomUUID();
        User member = new User("member", "member@example.com", "hashed-password");
        ReflectionTestUtils.setField(member, "id", memberId);

        League league = new League("Office League", owner, "AB23KLP9");
        ReflectionTestUtils.setField(league, "id", leagueId);

        LeagueResponse expectedResponse = new LeagueResponse(
                leagueId,
                "Office League",
                userId,
                2,
                List.of("owner", "member"),
                "AB23KLP9",
                Instant.parse("2026-09-12T20:00:00Z")
        );

        when(userRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(leagueRepository.findByJoinCode("AB23KLP9")).thenReturn(Optional.of(league));
        when(leagueMemberRepository.existsByLeagueIdAndUserId(leagueId, memberId))
                .thenReturn(false);
        when(leagueMemberRepository.findAllByLeagueId(leagueId))
                .thenReturn(List.of(new LeagueMember(league, owner), new LeagueMember(league, member)));
        when(leagueMapper.toResponse(league, 2L, List.of("owner", "member")))
                .thenReturn(expectedResponse);

        LeagueResponse response = leagueService.joinLeague(
                memberId,
                new JoinLeagueRequest(" ab23klp9 ")
        );

        assertThat(response).isEqualTo(expectedResponse);

        ArgumentCaptor<LeagueMember> memberCaptor =
                ArgumentCaptor.forClass(LeagueMember.class);
        verify(leagueMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getUser()).isEqualTo(member);
        assertThat(memberCaptor.getValue().getLeague()).isEqualTo(league);
    }

    @Test
    void joinLeague_rejectsInvalidJoinCode() {
        UUID memberId = UUID.randomUUID();
        User member = new User("member", "member@example.com", "hashed-password");
        ReflectionTestUtils.setField(member, "id", memberId);

        when(userRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(leagueRepository.findByJoinCode("UNKNOWN1")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                leagueService.joinLeague(memberId, new JoinLeagueRequest("UNKNOWN1"))
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Invalid join code");

        verify(leagueMemberRepository, never()).save(any(LeagueMember.class));
    }

    @Test
    void joinLeague_rejectsDuplicateMembership() {
        UUID memberId = UUID.randomUUID();
        UUID leagueId = UUID.randomUUID();
        User member = new User("member", "member@example.com", "hashed-password");
        ReflectionTestUtils.setField(member, "id", memberId);

        League league = new League("Office League", owner, "AB23KLP9");
        ReflectionTestUtils.setField(league, "id", leagueId);

        when(userRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(leagueRepository.findByJoinCode("AB23KLP9")).thenReturn(Optional.of(league));
        when(leagueMemberRepository.existsByLeagueIdAndUserId(leagueId, memberId))
                .thenReturn(true);

        assertThatThrownBy(() ->
                leagueService.joinLeague(memberId, new JoinLeagueRequest("AB23KLP9"))
        )
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("User is already a member of this league");

        verify(leagueMemberRepository, never()).save(any(LeagueMember.class));
    }

    @Test
    void leaveLeague_removesMembershipForNonOwner() {
        UUID memberId = UUID.randomUUID();
        UUID leagueId = UUID.randomUUID();
        League league = new League("Office League", owner, "AB23KLP9");
        ReflectionTestUtils.setField(league, "id", leagueId);

        User member = new User("member", "member@example.com", "hashed-password");
        ReflectionTestUtils.setField(member, "id", memberId);
        LeagueMember membership = new LeagueMember(league, member);

        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(league));
        when(leagueMemberRepository.findByLeagueIdAndUserId(leagueId, memberId))
                .thenReturn(Optional.of(membership));

        leagueService.leaveLeague(memberId, leagueId);

        verify(leagueMemberRepository).delete(membership);
    }

    @Test
    void leaveLeague_rejectsOwnerLeaving() {
        UUID leagueId = UUID.randomUUID();
        League league = new League("Office League", owner, "AB23KLP9");
        ReflectionTestUtils.setField(league, "id", leagueId);

        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(league));

        assertThatThrownBy(() -> leagueService.leaveLeague(userId, leagueId))
                .isInstanceOf(OwnerCannotLeaveException.class)
                .hasMessage("League owner cannot leave the league");

        verify(leagueMemberRepository, never()).delete(any(LeagueMember.class));
    }

    @Test
    void leaveLeague_rejectsMissingMembership() {
        UUID memberId = UUID.randomUUID();
        UUID leagueId = UUID.randomUUID();
        League league = new League("Office League", owner, "AB23KLP9");
        ReflectionTestUtils.setField(league, "id", leagueId);

        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(league));
        when(leagueMemberRepository.findByLeagueIdAndUserId(leagueId, memberId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> leagueService.leaveLeague(memberId, leagueId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Membership not found");
    }

    @Test
    void getLeaguesForUser_returnsMembershipLeagues() {
        UUID leagueId = UUID.randomUUID();
        League league = new League("Office League", owner, "AB23KLP9");
        ReflectionTestUtils.setField(league, "id", leagueId);
        LeagueMember membership = new LeagueMember(league, owner);
        LeagueResponse expectedResponse = new LeagueResponse(
                leagueId,
                "Office League",
                userId,
                1,
                List.of("owner"),
                "AB23KLP9",
                Instant.parse("2026-09-12T20:00:00Z")
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(leagueMemberRepository.findAllByUserId(userId))
                .thenReturn(List.of(membership));
        when(leagueMemberRepository.findAllByLeagueId(leagueId))
                .thenReturn(List.of(membership));
        when(leagueMapper.toResponse(league, 1L, List.of("owner")))
                .thenReturn(expectedResponse);

        List<LeagueResponse> response = leagueService.getLeaguesForUser(userId);

        assertThat(response).containsExactly(expectedResponse);
    }

    @Test
    void getLeague_returnsLeagueForMember() {
        UUID leagueId = UUID.randomUUID();
        League league = new League("Office League", owner, "AB23KLP9");
        ReflectionTestUtils.setField(league, "id", leagueId);
        LeagueResponse expectedResponse = new LeagueResponse(
                leagueId,
                "Office League",
                userId,
                1,
                List.of("owner"),
                "AB23KLP9",
                Instant.parse("2026-09-12T20:00:00Z")
        );

        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(league));
        when(leagueMemberRepository.existsByLeagueIdAndUserId(leagueId, userId))
                .thenReturn(true);
        when(leagueMemberRepository.findAllByLeagueId(leagueId))
                .thenReturn(List.of(new LeagueMember(league, owner)));
        when(leagueMapper.toResponse(league, 1L, List.of("owner")))
                .thenReturn(expectedResponse);

        LeagueResponse response = leagueService.getLeague(userId, leagueId);

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void getLeague_rejectsNonMember() {
        UUID nonMemberId = UUID.randomUUID();
        UUID leagueId = UUID.randomUUID();
        League league = new League("Office League", owner, "AB23KLP9");
        ReflectionTestUtils.setField(league, "id", leagueId);

        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(league));
        when(leagueMemberRepository.existsByLeagueIdAndUserId(leagueId, nonMemberId))
                .thenReturn(false);

        assertThatThrownBy(() -> leagueService.getLeague(nonMemberId, leagueId))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("User is not a member of this league");
    }

    @Test
    void updateLeague_updatesNameForOwner() {
        UUID leagueId = UUID.randomUUID();
        League league = new League("Office League", owner, "AB23KLP9");
        ReflectionTestUtils.setField(league, "id", leagueId);
        LeagueResponse expectedResponse = new LeagueResponse(
                leagueId,
                "Renamed League",
                userId,
                1,
                List.of("owner"),
                "AB23KLP9",
                Instant.parse("2026-09-12T20:00:00Z")
        );

        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(league));
        when(leagueMemberRepository.findAllByLeagueId(leagueId))
                .thenReturn(List.of(new LeagueMember(league, owner)));
        when(leagueMapper.toResponse(league, 1L, List.of("owner")))
                .thenReturn(expectedResponse);

        LeagueResponse response = leagueService.updateLeague(
                userId,
                leagueId,
                new UpdateLeagueRequest("Renamed League")
        );

        assertThat(league.getName()).isEqualTo("Renamed League");
        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void updateLeague_rejectsNonOwner() {
        UUID leagueId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        League league = new League("Office League", owner, "AB23KLP9");
        ReflectionTestUtils.setField(league, "id", leagueId);

        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(league));

        assertThatThrownBy(() -> leagueService.updateLeague(
                memberId,
                leagueId,
                new UpdateLeagueRequest("Renamed League")
        ))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Only the league owner can perform this operation");
    }

    @Test
    void removeMember_removesMemberForOwner() {
        UUID leagueId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        League league = new League("Office League", owner, "AB23KLP9");
        ReflectionTestUtils.setField(league, "id", leagueId);
        User member = new User("member", "member@example.com", "hashed-password");
        ReflectionTestUtils.setField(member, "id", memberId);
        LeagueMember membership = new LeagueMember(league, member);

        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(league));
        when(leagueMemberRepository.findByLeagueIdAndUserId(leagueId, memberId))
                .thenReturn(Optional.of(membership));

        leagueService.removeMember(userId, leagueId, memberId);

        verify(leagueMemberRepository).delete(membership);
    }

    @Test
    void removeMember_rejectsNonOwner() {
        UUID leagueId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        League league = new League("Office League", owner, "AB23KLP9");
        ReflectionTestUtils.setField(league, "id", leagueId);

        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(league));

        assertThatThrownBy(() -> leagueService.removeMember(memberId, leagueId, userId))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Only the league owner can perform this operation");

        verify(leagueMemberRepository, never()).delete(any(LeagueMember.class));
    }

    @Test
    void removeMember_rejectsRemovingOwner() {
        UUID leagueId = UUID.randomUUID();
        League league = new League("Office League", owner, "AB23KLP9");
        ReflectionTestUtils.setField(league, "id", leagueId);

        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(league));

        assertThatThrownBy(() -> leagueService.removeMember(userId, leagueId, userId))
                .isInstanceOf(OwnerCannotLeaveException.class)
                .hasMessage("League owner cannot be removed from the league");

        verify(leagueMemberRepository, never()).delete(any(LeagueMember.class));
    }
}

