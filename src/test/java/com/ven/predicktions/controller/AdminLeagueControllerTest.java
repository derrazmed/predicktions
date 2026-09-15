package com.ven.predicktions.controller;

import com.ven.predicktions.config.SecurityConfig;
import com.ven.predicktions.dto.league.AdminLeagueOwnerResponse;
import com.ven.predicktions.dto.league.AdminLeaguePageResponse;
import com.ven.predicktions.dto.league.AdminLeagueResponse;
import com.ven.predicktions.dto.league.AdminLeagueDetailsResponse;
import com.ven.predicktions.dto.league.AdminLeagueStatisticsResponse;
import com.ven.predicktions.dto.leaderboard.LeaderboardEntryResponse;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.exception.GlobalExceptionHandler;
import com.ven.predicktions.model.Role;
import com.ven.predicktions.security.JwtAuthenticationFilter;
import com.ven.predicktions.security.JwtService;
import com.ven.predicktions.service.AdminLeagueService;
import com.ven.predicktions.service.AdminLeagueDetailsService;
import com.ven.predicktions.service.AdminLeagueDeletionService;
import com.ven.predicktions.service.AdminLeagueMembershipService;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(AdminLeagueController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminLeagueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminLeagueService adminLeagueService;

    @MockitoBean
    private AdminLeagueDetailsService adminLeagueDetailsService;

    @MockitoBean
    private AdminLeagueDeletionService adminLeagueDeletionService;

    @MockitoBean
    private AdminLeagueMembershipService adminLeagueMembershipService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldReturnLeaguesForAdminWithoutSensitiveOwnerFields() throws Exception {
        UUID leagueId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(adminLeagueService.getLeagues(0, 20)).thenReturn(
                new AdminLeaguePageResponse(
                        List.of(new AdminLeagueResponse(
                                leagueId,
                                "Test League",
                                new AdminLeagueOwnerResponse(ownerId, "owner"),
                                2,
                                Instant.parse("2026-09-12T23:24:18.470425Z"),
                                "VBS3S3KA"
                        )),
                        0, 20, 1, 1
                )
        );

        mockMvc.perform(get("/api/admin/leagues")
                        .with(authentication(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(leagueId.toString()))
                .andExpect(jsonPath("$.content[0].name").value("Test League"))
                .andExpect(jsonPath("$.content[0].owner.id").value(ownerId.toString()))
                .andExpect(jsonPath("$.content[0].owner.username").value("owner"))
                .andExpect(jsonPath("$.content[0].memberCount").value(2))
                .andExpect(jsonPath("$.content[0].joinCode").value("VBS3S3KA"))
                .andExpect(jsonPath("$.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].token").doesNotExist())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldForwardPaginationParameters() throws Exception {
        when(adminLeagueService.getLeagues(2, 50))
                .thenReturn(new AdminLeaguePageResponse(List.of(), 2, 50, 0, 0));

        mockMvc.perform(get("/api/admin/leagues")
                        .param("page", "2")
                        .param("size", "50")
                        .with(authentication(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void shouldRejectNonAdminAndUnauthenticatedRequests() throws Exception {
        String path = "/api/admin/leagues";

        mockMvc.perform(get(path).with(authentication(Role.USER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnLeagueDetailsForAdminWithoutSensitiveFields() throws Exception {
        UUID leagueId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(adminLeagueDetailsService.getLeague(leagueId)).thenReturn(
                new AdminLeagueDetailsResponse(
                        leagueId,
                        "League",
                        Instant.parse("2026-09-12T23:24:18Z"),
                        "JOIN123",
                        1,
                        new AdminLeagueOwnerResponse(ownerId, "owner"),
                        List.of(),
                        List.of(new LeaderboardEntryResponse(1, ownerId, "owner", 10)),
                        new AdminLeagueStatisticsResponse(2, 1, 10, 5.0, 1, 1)
                )
        );

        mockMvc.perform(get("/api/admin/leagues/{leagueId}", leagueId)
                        .with(authentication(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(leagueId.toString()))
                .andExpect(jsonPath("$.owner.id").value(ownerId.toString()))
                .andExpect(jsonPath("$.leaderboard[0].rank").value(1))
                .andExpect(jsonPath("$.statistics.totalPredictions").value(2))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.owner.password").doesNotExist())
                .andExpect(jsonPath("$.owner.token").doesNotExist());
    }

    @Test
    void shouldReturnNotFoundForUnknownLeagueDetails() throws Exception {
        UUID leagueId = UUID.randomUUID();
        when(adminLeagueDetailsService.getLeague(leagueId))
                .thenThrow(new ResourceNotFoundException("League not found"));

        mockMvc.perform(get("/api/admin/leagues/{leagueId}", leagueId)
                        .with(authentication(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void shouldDeleteLeagueForAdmin() throws Exception {
        UUID leagueId = UUID.randomUUID();

        mockMvc.perform(delete("/api/admin/leagues/{leagueId}", leagueId)
                        .with(authentication(Role.ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectLeagueDeletionForNonAdminAndUnauthenticatedRequests() throws Exception {
        UUID leagueId = UUID.randomUUID();
        String path = "/api/admin/leagues/" + leagueId;

        mockMvc.perform(delete(path).with(authentication(Role.USER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(path))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRemoveLeagueMemberForAdmin() throws Exception {
        UUID leagueId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/admin/leagues/{leagueId}/members/{userId}", leagueId, userId)
                        .with(authentication(Role.ADMIN)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void shouldRejectMemberRemovalForNonAdminAndUnauthenticatedRequests() throws Exception {
        UUID leagueId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String path = "/api/admin/leagues/" + leagueId + "/members/" + userId;

        mockMvc.perform(delete(path).with(authentication(Role.USER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(path))
                .andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor authentication(Role role) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(
                        UUID.randomUUID(),
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_" + role.name())
                )
        );
    }
}
