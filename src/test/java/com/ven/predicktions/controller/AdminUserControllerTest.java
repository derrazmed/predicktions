package com.ven.predicktions.controller;

import com.ven.predicktions.dto.user.AdminUserPageResponse;
import com.ven.predicktions.dto.user.AdminUserResponse;
import com.ven.predicktions.dto.user.AdminUserDetailsResponse;
import com.ven.predicktions.dto.user.AdminLeagueSummary;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.exception.GlobalExceptionHandler;
import com.ven.predicktions.model.Role;
import com.ven.predicktions.security.JwtService;
import com.ven.predicktions.security.JwtAuthenticationFilter;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.config.SecurityConfig;
import com.ven.predicktions.service.AdminUserService;
import com.ven.predicktions.service.AdminPredictionService;
import com.ven.predicktions.service.AdminPointsService;
import com.ven.predicktions.dto.user.PointsAdjustmentResponse;
import com.ven.predicktions.dto.prediction.AdminPredictionPageResponse;
import com.ven.predicktions.dto.prediction.AdminPredictionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private AdminPredictionService adminPredictionService;

    @MockitoBean
    private AdminPointsService adminPointsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void shouldReturnUsersForAdminWithoutSensitiveFields() throws Exception {
        UUID id = UUID.randomUUID();
        when(adminUserService.getUsers(0, 20))
                .thenReturn(new AdminUserPageResponse(
                        List.of(new AdminUserResponse(
                                id,
                                "testuser",
                                "test@example.com",
                                Role.USER,
                                Instant.parse("2026-09-12T23:24:18.470425Z"),
                                true
                        )),
                        0,
                        20,
                        1,
                        1
                ));

        mockMvc.perform(get("/api/admin/users")
                        .with(adminAuthentication(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].username").value("testuser"))
                .andExpect(jsonPath("$.content[0].email").value("test@example.com"))
                .andExpect(jsonPath("$.content[0].role").value("USER"))
                .andExpect(jsonPath("$.content[0].enabled").value(true))
                .andExpect(jsonPath("$.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].accessToken").doesNotExist())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldRejectUser() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .with(adminAuthentication(Role.USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldPassRequestedPaginationToService() throws Exception {
        when(adminUserService.getUsers(2, 10))
                .thenReturn(new AdminUserPageResponse(List.of(), 2, 10, 0, 0));

        mockMvc.perform(get("/api/admin/users?page=2&size=10")
                        .with(adminAuthentication(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void shouldReturnUserDetailsWithoutSensitiveFields() throws Exception {
        UUID id = UUID.randomUUID();
        when(adminUserService.getUser(id))
                .thenReturn(new AdminUserDetailsResponse(
                        id,
                        "testuser",
                        "test@example.com",
                        Role.USER,
                        Instant.parse("2026-09-12T23:24:18.470425Z"),
                        true,
                        42,
                        127,
                        List.of(new AdminLeagueSummary(
                                UUID.randomUUID(),
                                "mo3a9in"
                        )),
                        8
                ));

        mockMvc.perform(get("/api/admin/users/" + id)
                        .with(adminAuthentication(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.predictionCount").value(42))
                .andExpect(jsonPath("$.points").value(127))
                .andExpect(jsonPath("$.leagues[0].name").value("mo3a9in"))
                .andExpect(jsonPath("$.leaderboardPosition").value(8))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void shouldReturnNotFoundForUnknownUser() throws Exception {
        UUID id = UUID.randomUUID();
        when(adminUserService.getUser(id))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/admin/users/" + id)
                        .with(adminAuthentication(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void shouldAwardPointsForAdminUsingAuthenticatedIdentity() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        when(adminPointsService.adjustPoints(
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(adminId)
        )).thenReturn(new PointsAdjustmentResponse(
                userId, "testuser", 10,
                "Correction", adminId,
                Instant.parse("2026-09-14T12:30:00Z")
        ));

        mockMvc.perform(post("/api/admin/users/" + userId + "/points")
                        .contentType("application/json")
                        .content("{\"points\":10,\"reason\":\"Correction\"}")
                        .with(adminAuthentication(Role.ADMIN, adminId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.points").value(10))
                .andExpect(jsonPath("$.awardedBy").value(adminId.toString()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void shouldRejectPointAwardForUserAndUnauthenticatedCaller() throws Exception {
        String path = "/api/admin/users/" + UUID.randomUUID() + "/points";
        String body = "{\"points\":10,\"reason\":\"Correction\"}";

        mockMvc.perform(post(path).contentType("application/json").content(body)
                        .with(adminAuthentication(Role.USER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(path).contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInvalidPointAwardPayloads() throws Exception {
        String path = "/api/admin/users/" + UUID.randomUUID() + "/points";
        var admin = adminAuthentication(Role.ADMIN);

        mockMvc.perform(post(path).contentType("application/json")
                        .content("{}").with(admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(path).contentType("application/json")
                        .content("{\"points\":0,\"reason\":\"Correction\"}").with(admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(path).contentType("application/json")
                        .content("{\"points\":1,\"reason\":\"   \"}").with(admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(path).contentType("application/json")
                        .content("{\"points\":1}").with(admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(path).contentType("application/json")
                        .content("{\"points\":1,\"reason\":\"x\"").with(admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAcceptNegativePointAdjustmentForAdmin() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        when(adminPointsService.adjustPoints(
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(adminId)
        )).thenReturn(new PointsAdjustmentResponse(
                userId, "testuser", -5, "Manual correction", adminId,
                Instant.parse("2026-09-14T12:30:00Z")
        ));

        mockMvc.perform(post("/api/admin/users/" + userId + "/points")
                        .contentType("application/json")
                        .content("{\"points\":-5,\"reason\":\"Manual correction\"}")
                        .with(adminAuthentication(Role.ADMIN, adminId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").value(-5))
                .andExpect(jsonPath("$.reason").value("Manual correction"));
    }

    @Test
    void shouldRejectPointAdjustmentOutsideConfiguredBounds() throws Exception {
        String path = "/api/admin/users/" + UUID.randomUUID() + "/points";

        mockMvc.perform(post(path).contentType("application/json")
                        .content("{\"points\":1000001,\"reason\":\"Correction\"}")
                        .with(adminAuthentication(Role.ADMIN)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(path).contentType("application/json")
                        .content("{\"points\":-1000001,\"reason\":\"Correction\"}")
                        .with(adminAuthentication(Role.ADMIN)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(path).contentType("application/json")
                        .content("{\"points\":-5,\"reason\":\"\"}")
                        .with(adminAuthentication(Role.ADMIN)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(path).contentType("application/json")
                        .content("{\"points\":-5,\"reason\":\"" + "x".repeat(501) + "\"}")
                        .with(adminAuthentication(Role.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldChangeUserRoleForAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        when(adminUserService.changeUserRole(id, Role.ADMIN))
                .thenReturn(new AdminUserResponse(
                        id,
                        "testuser",
                        "test@example.com",
                        Role.ADMIN,
                        Instant.parse("2026-09-12T23:24:18.470425Z"),
                        true
                ));

        mockMvc.perform(patch("/api/admin/users/" + id + "/role")
                        .contentType("application/json")
                        .content("{\"role\":\"ADMIN\"}")
                        .with(adminAuthentication(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void shouldRejectRoleChangeForUser() throws Exception {
        mockMvc.perform(patch("/api/admin/users/" + UUID.randomUUID() + "/role")
                        .contentType("application/json")
                        .content("{\"role\":\"ADMIN\"}")
                        .with(adminAuthentication(Role.USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectUnauthenticatedRoleChange() throws Exception {
        mockMvc.perform(patch("/api/admin/users/" + UUID.randomUUID() + "/role")
                        .contentType("application/json")
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInvalidRolePayloads() throws Exception {
        mockMvc.perform(patch("/api/admin/users/" + UUID.randomUUID() + "/role")
                        .contentType("application/json")
                        .content("{\"role\":\"SUPER_ADMIN\"}")
                        .with(adminAuthentication(Role.ADMIN)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/admin/users/" + UUID.randomUUID() + "/role")
                        .contentType("application/json")
                        .content("{}")
                        .with(adminAuthentication(Role.ADMIN)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/admin/users/" + UUID.randomUUID() + "/role")
                        .contentType("application/json")
                        .content("{\"role\":null}")
                        .with(adminAuthentication(Role.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldChangeUserStatusForAdminWithoutSensitiveFields() throws Exception {
        UUID id = UUID.randomUUID();
        when(adminUserService.setUserEnabled(id, false))
                .thenReturn(new AdminUserResponse(
                        id, "testuser", "test@example.com", Role.USER,
                        Instant.parse("2026-09-12T23:24:18.470425Z"), false
                ));

        mockMvc.perform(patch("/api/admin/users/" + id + "/status")
                        .contentType("application/json")
                        .content("{\"enabled\":false}")
                        .with(adminAuthentication(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void shouldRejectInvalidStatusPayloads() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/api/admin/users/" + id + "/status")
                        .contentType("application/json")
                        .content("{}")
                        .with(adminAuthentication(Role.ADMIN)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/admin/users/" + id + "/status")
                        .contentType("application/json")
                        .content("{\"enabled\":null}")
                        .with(adminAuthentication(Role.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectStatusChangeForUserAndUnauthenticatedCaller() throws Exception {
        String path = "/api/admin/users/" + UUID.randomUUID() + "/status";
        mockMvc.perform(patch(path).contentType("application/json")
                        .content("{\"enabled\":false}")
                        .with(adminAuthentication(Role.USER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(path).contentType("application/json")
                        .content("{\"enabled\":false}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnUserPredictionHistoryForAdmin() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID predictionId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        when(adminPredictionService.getUserPredictions(userId, 0, 20))
                .thenReturn(new AdminPredictionPageResponse(
                        List.of(new AdminPredictionResponse(
                                predictionId, userId, "testuser", matchId, 3,
                                2, 1, 3,
                                Instant.parse("2026-09-13T10:30:00Z"),
                                Instant.parse("2026-09-13T10:30:00Z")
                        )),
                        0, 20, 1, 1
                ));

        mockMvc.perform(get("/api/admin/users/" + userId + "/predictions")
                        .with(adminAuthentication(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(predictionId.toString()))
                .andExpect(jsonPath("$.content[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.content[0].username").value("testuser"))
                .andExpect(jsonPath("$.content[0].matchId").value(matchId.toString()))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].token").doesNotExist());
    }

    @Test
    void shouldRejectUserPredictionHistoryForUserAndUnauthenticatedCaller()
            throws Exception {
        String path = "/api/admin/users/" + UUID.randomUUID() + "/predictions";

        mockMvc.perform(get(path).with(adminAuthentication(Role.USER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldPassUserPredictionHistoryPaginationToService() throws Exception {
        UUID userId = UUID.randomUUID();
        when(adminPredictionService.getUserPredictions(userId, 2, 10))
                .thenReturn(new AdminPredictionPageResponse(
                        List.of(), 2, 10, 0, 0
                ));

        mockMvc.perform(get("/api/admin/users/" + userId + "/predictions?page=2&size=10")
                        .with(adminAuthentication(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void shouldReturnEmptyHistoryForExistingUserWithoutPredictions() throws Exception {
        UUID userId = UUID.randomUUID();
        when(adminPredictionService.getUserPredictions(userId, 0, 20))
                .thenReturn(new AdminPredictionPageResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/admin/users/" + userId + "/predictions")
                        .with(adminAuthentication(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void shouldReturnNotFoundForUnknownUserPredictionHistory() throws Exception {
        UUID userId = UUID.randomUUID();
        when(adminPredictionService.getUserPredictions(userId, 0, 20))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/admin/users/" + userId + "/predictions")
                        .with(adminAuthentication(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    private RequestPostProcessor
    adminAuthentication(Role role) {
        return adminAuthentication(role, UUID.randomUUID());
    }

    private RequestPostProcessor adminAuthentication(Role role, UUID userId) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_" + role.name())
                )
        );
    }
}
