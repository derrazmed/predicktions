package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.league.CreateLeagueRequest;
import com.ven.predicktions.dto.league.LeagueResponse;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.mapper.LeagueMapper;
import com.ven.predicktions.model.League;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.LeagueRepository;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.JoinCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
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
                userRepository,
                joinCodeGenerator,
                leagueMapper
        );

        userId = UUID.randomUUID();
        owner = new User("owner", "owner@example.com", "hashed-password");
    }

    @Test
    void createLeague_assignsOwnerAndGeneratedJoinCode() {
        CreateLeagueRequest request = new CreateLeagueRequest("Office League");
        LeagueResponse expectedResponse = new LeagueResponse(
                UUID.randomUUID(),
                "Office League",
                userId,
                "AB23KLP9",
                Instant.parse("2026-09-12T20:00:00Z")
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(joinCodeGenerator.generate()).thenReturn("AB23KLP9");
        when(leagueRepository.existsByJoinCode("AB23KLP9")).thenReturn(false);
        when(leagueRepository.save(any(League.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(leagueMapper.toResponse(any(League.class))).thenReturn(expectedResponse);

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
                "FREECODE",
                Instant.parse("2026-09-12T20:00:00Z")
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(joinCodeGenerator.generate()).thenReturn("TAKEN123", "FREECODE");
        when(leagueRepository.existsByJoinCode("TAKEN123")).thenReturn(true);
        when(leagueRepository.existsByJoinCode("FREECODE")).thenReturn(false);
        when(leagueRepository.save(any(League.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(leagueMapper.toResponse(any(League.class))).thenReturn(expectedResponse);

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
}
