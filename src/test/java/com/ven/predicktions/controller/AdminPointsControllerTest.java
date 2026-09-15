package com.ven.predicktions.controller;

import com.ven.predicktions.config.SecurityConfig;
import com.ven.predicktions.dto.user.AdminPointsAdjustmentPageResponse;
import com.ven.predicktions.dto.user.AdminPointsAdjustmentResponse;
import com.ven.predicktions.dto.user.AdjustmentHistoryFilter;
import com.ven.predicktions.exception.GlobalExceptionHandler;
import com.ven.predicktions.model.Role;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.security.JwtAuthenticationFilter;
import com.ven.predicktions.security.JwtService;
import com.ven.predicktions.service.AdminPointsService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminPointsController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminPointsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminPointsService adminPointsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void shouldReturnFilteredAdjustmentsForAdmin() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        when(adminPointsService.getAllAdjustmentHistory(
                eq(new AdjustmentHistoryFilter(
                        userId,
                        adminId,
                        Instant.parse("2026-09-01T00:00:00Z"),
                        Instant.parse("2026-09-15T23:59:59Z")
                )),
                eq(0),
                eq(20)
        )).thenReturn(new AdminPointsAdjustmentPageResponse(
                List.of(new AdminPointsAdjustmentResponse(
                        UUID.randomUUID(), userId, adminId, -5, "Correction",
                        Instant.parse("2026-09-14T12:30:00Z")
                )),
                0, 20, 1, 1
        ));

        mockMvc.perform(get("/api/admin/points/adjustments")
                        .param("userId", userId.toString())
                        .param("adminId", adminId.toString())
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-15T23:59:59Z")
                        .with(authentication(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.content[0].adminId").value(adminId.toString()))
                .andExpect(jsonPath("$.content[0].points").value(-5))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

    @Test
    void shouldUseDefaultPaginationAndRejectInvalidFilters() throws Exception {
        when(adminPointsService.getAllAdjustmentHistory(
                eq(new AdjustmentHistoryFilter(null, null, null, null)),
                eq(0), eq(20)
        )).thenReturn(new AdminPointsAdjustmentPageResponse(
                List.of(), 0, 20, 0, 0
        ));

        mockMvc.perform(get("/api/admin/points/adjustments")
                        .with(authentication(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));

        mockMvc.perform(get("/api/admin/points/adjustments")
                        .param("userId", "not-a-uuid")
                        .with(authentication(Role.ADMIN)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/points/adjustments")
                        .param("from", "2026-09-16T00:00:00Z")
                        .param("to", "2026-09-15T00:00:00Z")
                        .with(authentication(Role.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRequireAdminRole() throws Exception {
        String path = "/api/admin/points/adjustments";
        mockMvc.perform(get(path).with(authentication(Role.USER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor authentication(
            Role role
    ) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(
                        UUID.randomUUID(),
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_" + role.name())
                )
        );
    }
}
