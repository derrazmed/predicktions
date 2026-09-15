package com.ven.predicktions.controller;

import com.ven.predicktions.config.SecurityConfig;
import com.ven.predicktions.dto.dashboard.AdminDashboardResponse;
import com.ven.predicktions.security.JwtAuthenticationFilter;
import com.ven.predicktions.security.JwtService;
import com.ven.predicktions.service.AdminDashboardService;
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

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDashboardController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldReturnDashboardForAdminWithoutSensitiveFields() throws Exception {
        when(adminDashboardService.getDashboard()).thenReturn(
                new AdminDashboardResponse(152, 121, 2, 34, 4821, 120, 87)
        );

        mockMvc.perform(get("/api/admin/dashboard")
                        .with(authentication("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").value(152))
                .andExpect(jsonPath("$.activeUsers").value(121))
                .andExpect(jsonPath("$.admins").value(2))
                .andExpect(jsonPath("$.predictionsToday").value(87))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void shouldRejectUserAndUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .with(authentication("ROLE_USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/dashboard"))
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
