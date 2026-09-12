package com.ven.predicktions.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ven.predicktions.dto.league.CreateLeagueRequest;
import com.ven.predicktions.dto.league.LeagueResponse;
import com.ven.predicktions.exception.GlobalExceptionHandler;
import com.ven.predicktions.security.JwtService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
