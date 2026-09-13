package com.ven.predicktions.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ven.predicktions.dto.league.CreateLeagueRequest;
import com.ven.predicktions.dto.league.JoinLeagueRequest;
import com.ven.predicktions.dto.league.LeagueResponse;
import com.ven.predicktions.dto.league.UpdateLeagueRequest;
import com.ven.predicktions.exception.DuplicateResourceException;
import com.ven.predicktions.exception.ForbiddenOperationException;
import com.ven.predicktions.exception.GlobalExceptionHandler;
import com.ven.predicktions.exception.OwnerCannotLeaveException;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.security.JwtService;
import com.ven.predicktions.service.LeaderboardService;
import com.ven.predicktions.service.LeagueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeagueController.class)
@Import(GlobalExceptionHandler.class)
class LeagueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LeagueService leagueService;

    @MockitoBean
    private LeaderboardService leaderboardService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldCreateLeagueForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID leagueId = UUID.randomUUID();
        CreateLeagueRequest request = new CreateLeagueRequest("Office League");
        LeagueResponse response = new LeagueResponse(
                leagueId,
                "Office League",
                userId,
                1,
                List.of("owner"),
                "AB23KLP9",
                Instant.parse("2026-09-12T20:00:00Z")
        );

        when(leagueService.createLeague(eq(userId), any(CreateLeagueRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/leagues")
                                .with(authentication(userId))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(leagueId.toString()))
                .andExpect(jsonPath("$.name").value("Office League"))
                .andExpect(jsonPath("$.ownerId").value(userId.toString()))
                .andExpect(jsonPath("$.joinCode").value("AB23KLP9"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        CreateLeagueRequest request = new CreateLeagueRequest("Office League");

        mockMvc.perform(
                        post("/api/leagues")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());

        verify(leagueService, never()).createLeague(any(), any());
    }

    @Test
    void shouldRejectInvalidLeagueName() throws Exception {
        UUID userId = UUID.randomUUID();
        CreateLeagueRequest request = new CreateLeagueRequest("ab");

        mockMvc.perform(
                        post("/api/leagues")
                                .with(authentication(userId))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        verify(leagueService, never()).createLeague(any(), any());
    }

    @Test
    void shouldJoinLeagueForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID leagueId = UUID.randomUUID();
        JoinLeagueRequest request = new JoinLeagueRequest("AB23KLP9");
        LeagueResponse response = new LeagueResponse(
                leagueId,
                "Office League",
                UUID.randomUUID(),
                2,
                List.of("owner", "member"),
                "AB23KLP9",
                Instant.parse("2026-09-12T20:00:00Z")
        );

        when(leagueService.joinLeague(eq(userId), any(JoinLeagueRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/leagues/join")
                                .with(authentication(userId))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(leagueId.toString()))
                .andExpect(jsonPath("$.joinCode").value("AB23KLP9"));
    }

    @Test
    void shouldRejectUnauthenticatedJoin() throws Exception {
        JoinLeagueRequest request = new JoinLeagueRequest("AB23KLP9");

        mockMvc.perform(
                        post("/api/leagues/join")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());

        verify(leagueService, never()).joinLeague(any(), any());
    }

    @Test
    void shouldRejectBlankJoinCode() throws Exception {
        UUID userId = UUID.randomUUID();
        JoinLeagueRequest request = new JoinLeagueRequest("  ");

        mockMvc.perform(
                        post("/api/leagues/join")
                                .with(authentication(userId))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        verify(leagueService, never()).joinLeague(any(), any());
    }

    @Test
    void shouldRejectInvalidJoinCode() throws Exception {
        UUID userId = UUID.randomUUID();
        JoinLeagueRequest request = new JoinLeagueRequest("UNKNOWN1");

        when(leagueService.joinLeague(eq(userId), any(JoinLeagueRequest.class)))
                .thenThrow(new ResourceNotFoundException("Invalid join code"));

        mockMvc.perform(
                        post("/api/leagues/join")
                                .with(authentication(userId))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Invalid join code"));
    }

    @Test
    void shouldRejectJoiningTheSameLeagueTwice() throws Exception {
        UUID userId = UUID.randomUUID();
        JoinLeagueRequest request = new JoinLeagueRequest("AB23KLP9");

        when(leagueService.joinLeague(eq(userId), any(JoinLeagueRequest.class)))
                .thenThrow(new DuplicateResourceException(
                        "User is already a member of this league"
                ));

        mockMvc.perform(
                        post("/api/leagues/join")
                                .with(authentication(userId))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("RESOURCE_ALREADY_EXISTS"));
    }

    @Test
    void shouldLeaveLeagueForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID leagueId = UUID.randomUUID();

        mockMvc.perform(
                        delete("/api/leagues/{leagueId}/membership", leagueId)
                                .with(authentication(userId))
                                .with(csrf())
                )
                .andExpect(status().isNoContent());

        verify(leagueService).leaveLeague(userId, leagueId);
    }

    @Test
    void shouldRejectUnauthenticatedLeave() throws Exception {
        UUID leagueId = UUID.randomUUID();

        mockMvc.perform(
                        delete("/api/leagues/{leagueId}/membership", leagueId)
                                .with(csrf())
                )
                .andExpect(status().isUnauthorized());

        verify(leagueService, never()).leaveLeague(any(), any());
    }

    @Test
    void shouldRejectOwnerLeavingLeague() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID leagueId = UUID.randomUUID();

        doThrow(new OwnerCannotLeaveException("League owner cannot leave the league"))
                .when(leagueService)
                .leaveLeague(userId, leagueId);

        mockMvc.perform(
                        delete("/api/leagues/{leagueId}/membership", leagueId)
                                .with(authentication(userId))
                                .with(csrf())
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("OWNER_CANNOT_LEAVE"))
                .andExpect(jsonPath("$.message").value(
                        "League owner cannot leave the league"
                ));
    }

    @Test
    void shouldRetrieveLeaguesForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID leagueId = UUID.randomUUID();
        LeagueResponse response = new LeagueResponse(
                leagueId,
                "Office League",
                userId,
                2,
                List.of("owner", "member"),
                "AB23KLP9",
                Instant.parse("2026-09-12T20:00:00Z")
        );

        when(leagueService.getLeaguesForUser(userId)).thenReturn(List.of(response));

        mockMvc.perform(
                        get("/api/leagues")
                                .with(authentication(userId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(leagueId.toString()))
                .andExpect(jsonPath("$[0].name").value("Office League"))
                .andExpect(jsonPath("$[0].ownerId").value(userId.toString()))
                .andExpect(jsonPath("$[0].memberCount").value(2))
                .andExpect(jsonPath("$[0].memberUsernames[0]").value("owner"))
                .andExpect(jsonPath("$[0].memberUsernames[1]").value("member"))
                .andExpect(jsonPath("$[0].joinCode").value("AB23KLP9"));
    }

    @Test
    void shouldRetrieveLeagueDetailsForMember() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID leagueId = UUID.randomUUID();
        LeagueResponse response = new LeagueResponse(
                leagueId,
                "Office League",
                userId,
                2,
                List.of("owner", "member"),
                "AB23KLP9",
                Instant.parse("2026-09-12T20:00:00Z")
        );

        when(leagueService.getLeague(userId, leagueId)).thenReturn(response);

        mockMvc.perform(
                        get("/api/leagues/{leagueId}", leagueId)
                                .with(authentication(userId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(leagueId.toString()))
                .andExpect(jsonPath("$.memberCount").value(2))
                .andExpect(jsonPath("$.memberUsernames[0]").value("owner"))
                .andExpect(jsonPath("$.memberUsernames[1]").value("member"));
    }

    @Test
    void shouldRejectLeagueDetailsForNonMember() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID leagueId = UUID.randomUUID();

        when(leagueService.getLeague(userId, leagueId))
                .thenThrow(new ForbiddenOperationException(
                        "User is not a member of this league"
                ));

        mockMvc.perform(
                        get("/api/leagues/{leagueId}", leagueId)
                                .with(authentication(userId))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void shouldUpdateLeagueNameForOwner() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID leagueId = UUID.randomUUID();
        UpdateLeagueRequest request = new UpdateLeagueRequest("Renamed League");
        LeagueResponse response = new LeagueResponse(
                leagueId,
                "Renamed League",
                userId,
                2,
                List.of("owner", "member"),
                "AB23KLP9",
                Instant.parse("2026-09-12T20:00:00Z")
        );

        when(leagueService.updateLeague(
                eq(userId),
                eq(leagueId),
                any(UpdateLeagueRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        patch("/api/leagues/{leagueId}", leagueId)
                                .with(authentication(userId))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed League"));
    }

    @Test
    void shouldRejectInvalidLeagueUpdateName() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID leagueId = UUID.randomUUID();
        UpdateLeagueRequest request = new UpdateLeagueRequest("ab");

        mockMvc.perform(
                        patch("/api/leagues/{leagueId}", leagueId)
                                .with(authentication(userId))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        verify(leagueService, never()).updateLeague(any(), any(), any());
    }

    @Test
    void shouldRejectLeagueUpdateForNonOwner() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID leagueId = UUID.randomUUID();

        when(leagueService.updateLeague(
                eq(userId),
                eq(leagueId),
                any(UpdateLeagueRequest.class)
        )).thenThrow(new ForbiddenOperationException(
                "Only the league owner can perform this operation"
        ));

        mockMvc.perform(
                        patch("/api/leagues/{leagueId}", leagueId)
                                .with(authentication(userId))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new UpdateLeagueRequest("Renamed League")
                                ))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void shouldRemoveMemberForOwner() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID leagueId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        mockMvc.perform(
                        delete(
                                "/api/leagues/{leagueId}/members/{memberId}",
                                leagueId,
                                memberId
                        )
                                .with(authentication(ownerId))
                                .with(csrf())
                )
                .andExpect(status().isNoContent());

        verify(leagueService).removeMember(ownerId, leagueId, memberId);
    }

    @Test
    void shouldRejectRemoveMemberForNonOwner() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID leagueId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        doThrow(new ForbiddenOperationException(
                "Only the league owner can perform this operation"
        ))
                .when(leagueService)
                .removeMember(userId, leagueId, memberId);

        mockMvc.perform(
                        delete(
                                "/api/leagues/{leagueId}/members/{memberId}",
                                leagueId,
                                memberId
                        )
                                .with(authentication(userId))
                                .with(csrf())
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    private RequestPostProcessor authentication(UUID userId) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        AuthorityUtils.NO_AUTHORITIES
                )
        );
    }
}
