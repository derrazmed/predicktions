package com.ven.predicktions.controller;

import com.ven.predicktions.config.SecurityConfig;
import com.ven.predicktions.dto.prediction.AdminPredictionPageResponse;
import com.ven.predicktions.dto.prediction.AdminPredictionResponse;
import com.ven.predicktions.exception.GlobalExceptionHandler;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.model.Role;
import com.ven.predicktions.security.JwtAuthenticationFilter;
import com.ven.predicktions.security.JwtService;
import com.ven.predicktions.service.AdminPredictionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminMatchController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminMatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminPredictionService adminPredictionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private com.ven.predicktions.repository.UserRepository userRepository;

    @Test
    void shouldReturnMatchPredictionsForAdmin() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID predictionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(adminPredictionService.getMatchPredictions(matchId, 1, 50))
                .thenReturn(new AdminPredictionPageResponse(
                        List.of(new AdminPredictionResponse(
                                predictionId,
                                userId,
                                "testuser",
                                matchId,
                                3,
                                2,
                                1,
                                3,
                                Instant.parse("2026-09-13T10:30:00Z"),
                                Instant.parse("2026-09-13T10:30:00Z")
                        )),
                        1,
                        50,
                        1,
                        1
                ));

        mockMvc.perform(get("/api/admin/matches/{matchId}/predictions", matchId)
                        .param("page", "1")
                        .param("size", "50")
                        .with(authentication(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(predictionId.toString()))
                .andExpect(jsonPath("$.content[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.content[0].username").value("testuser"))
                .andExpect(jsonPath("$.content[0].matchId").value(matchId.toString()))
                .andExpect(jsonPath("$.content[0].predictedHomeScore").value(2))
                .andExpect(jsonPath("$.content[0].predictedAwayScore").value(1))
                .andExpect(jsonPath("$.content[0].points").value(3))
                .andExpect(jsonPath("$.content[0].createdAt").value("2026-09-13T10:30:00Z"))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].token").doesNotExist())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    void shouldUseDefaultPagination() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(adminPredictionService.getMatchPredictions(matchId, 0, 20))
                .thenReturn(new AdminPredictionPageResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/admin/matches/{matchId}/predictions", matchId)
                        .with(authentication(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void shouldReturnNotFoundForUnknownMatch() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(adminPredictionService.getMatchPredictions(matchId, 0, 20))
                .thenThrow(new ResourceNotFoundException("Match not found"));

        mockMvc.perform(get("/api/admin/matches/{matchId}/predictions", matchId)
                        .with(authentication(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Match not found"));
    }

    @Test
    void shouldRejectNonAdminAndUnauthenticatedRequests() throws Exception {
        UUID matchId = UUID.randomUUID();
        String path = "/api/admin/matches/" + matchId + "/predictions";

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
