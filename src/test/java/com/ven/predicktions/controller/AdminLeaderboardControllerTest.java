package com.ven.predicktions.controller;

import com.ven.predicktions.config.SecurityConfig;
import com.ven.predicktions.dto.leaderboard.LeaderboardEntryResponse;
import com.ven.predicktions.exception.GlobalExceptionHandler;
import com.ven.predicktions.security.JwtAuthenticationFilter;
import com.ven.predicktions.security.JwtService;
import com.ven.predicktions.service.LeaderboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminLeaderboardController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminLeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaderboardService leaderboardService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldReturnGlobalLeaderboardWithFiltersForAdmin() throws Exception {
        UUID userId = UUID.randomUUID();
        when(leaderboardService.getGlobalLeaderboard(3, 2026))
                .thenReturn(List.of(new LeaderboardEntryResponse(1, userId, "alice", 42)));

        mockMvc.perform(get("/api/admin/leaderboard")
                        .param("gameweek", "3")
                        .param("season", "2026")
                        .with(authentication("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].totalPoints").value(42))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void shouldRejectInvalidGameweek() throws Exception {
        when(leaderboardService.getGlobalLeaderboard(0, null))
                .thenThrow(new IllegalArgumentException("gameweek must be between 1 and 53"));

        mockMvc.perform(get("/api/admin/leaderboard")
                        .param("gameweek", "0")
                        .with(authentication("ROLE_ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectUserAndUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/admin/leaderboard")
                        .with(authentication("ROLE_USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/leaderboard"))
                .andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor authentication(String authority) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(
                        UUID.randomUUID(),
                        null,
                        AuthorityUtils.createAuthorityList(authority)
                )
        );
    }
}
