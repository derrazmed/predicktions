package com.ven.predicktions.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ven.predicktions.dto.auth.RegisterRequest;
import com.ven.predicktions.dto.auth.RegisterResponse;
import com.ven.predicktions.exception.DuplicateResourceException;
import com.ven.predicktions.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(com.ven.predicktions.exception.GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @WithMockUser
    void shouldRegisterUser() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "derrazz",
                "user@example.com",
                "password123"
        );

        RegisterResponse response = new RegisterResponse(
                UUID.randomUUID(),
                "derrazz",
                "user@example.com",
                Instant.now()
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("derrazz"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @WithMockUser
    void shouldRejectInvalidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "",
                "invalid-email",
                "short"
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid request"));
    }

    @Test
    @WithMockUser
    void shouldRejectDuplicateUsername() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "derrazz",
                "user@example.com",
                "password123"
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("Username is already in use"));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("RESOURCE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Username is already in use"));
    }

    @Test
    @WithMockUser
    void shouldRejectDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "derrazz",
                "user@example.com",
                "password123"
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("Email is already in use"));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("RESOURCE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Email is already in use"));
    }

    @Test
    @WithMockUser
    void shouldNotExposePasswordInResponse() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "derrazz",
                "user@example.com",
                "password123"
        );

        RegisterResponse response = new RegisterResponse(
                UUID.randomUUID(),
                "derrazz",
                "user@example.com",
                Instant.now()
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
}