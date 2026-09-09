package com.ven.predicktions.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ven.predicktions.dto.user.UserResponse;
import com.ven.predicktions.exception.GlobalExceptionHandler;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.security.JwtService;
import com.ven.predicktions.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldReturnCurrentUser() throws Exception {
        UUID userId = UUID.randomUUID();

        UserResponse response = new UserResponse(
                userId,
                "derrazz",
                "user@example.com"
        );

        when(userService.getCurrentUser(userId))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/users/me")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.authentication(
                                                new UsernamePasswordAuthenticationToken(
                                                        userId,
                                                        null,
                                                        AuthorityUtils.NO_AUTHORITIES
                                                )
                                        )
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("derrazz"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(
                        get("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        UUID userId = UUID.randomUUID();

        when(userService.getCurrentUser(userId))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(
                        get("/api/users/me")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.authentication(
                                                new UsernamePasswordAuthenticationToken(
                                                        userId,
                                                        null,
                                                        AuthorityUtils.NO_AUTHORITIES
                                                )
                                        )
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("User not found"));
    }
}