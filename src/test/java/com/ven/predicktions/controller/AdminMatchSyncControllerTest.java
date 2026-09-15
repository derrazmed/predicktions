package com.ven.predicktions.controller;

import com.ven.predicktions.config.SecurityConfig;
import com.ven.predicktions.config.SportsSyncProperties;
import com.ven.predicktions.dto.match.MatchSyncResponse;
import com.ven.predicktions.exception.GlobalExceptionHandler;
import com.ven.predicktions.model.Role;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.security.JwtAuthenticationFilter;
import com.ven.predicktions.security.JwtService;
import com.ven.predicktions.service.MatchSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminMatchSyncController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminMatchSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchSyncService matchSyncService;

    @MockitoBean
    private SportsSyncProperties properties;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void shouldAllowAdminToTriggerSynchronization() throws Exception {
        when(properties.getCompetitionCode()).thenReturn("PL");
        when(matchSyncService.synchronizeManually(any()))
                .thenReturn(new MatchSyncResponse(1, 1, 0, 0, 0, Instant.now()));

        mockMvc.perform(post("/api/admin/matches/sync")
                        .with(authentication(Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectUserAndUnauthenticatedSynchronization() throws Exception {
        String path = "/api/admin/matches/sync";
        mockMvc.perform(post(path).with(authentication(Role.USER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(path))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInvalidDateRange() throws Exception {
        mockMvc.perform(post("/api/admin/matches/sync")
                        .param("from", "2026-09-15")
                        .param("to", "2026-09-01")
                        .with(authentication(Role.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor authentication(
            Role role
    ) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(
                        java.util.UUID.randomUUID(), null,
                        AuthorityUtils.createAuthorityList("ROLE_" + role.name())
                )
        );
    }
}
