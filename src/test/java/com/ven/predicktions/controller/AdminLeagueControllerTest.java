package com.ven.predicktions.controller;

import com.ven.predicktions.config.SecurityConfig;
import com.ven.predicktions.dto.league.AdminLeagueOwnerResponse;
import com.ven.predicktions.dto.league.AdminLeaguePageResponse;
import com.ven.predicktions.dto.league.AdminLeagueResponse;
import com.ven.predicktions.exception.GlobalExceptionHandler;
import com.ven.predicktions.model.Role;
import com.ven.predicktions.security.JwtAuthenticationFilter;
import com.ven.predicktions.security.JwtService;
import com.ven.predicktions.service.AdminLeagueService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminLeagueController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminLeagueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminLeagueService adminLeagueService;

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
