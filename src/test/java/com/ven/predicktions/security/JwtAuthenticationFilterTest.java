package com.ven.predicktions.security;

import com.ven.predicktions.model.Role;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.UserRepository;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateUserWithUserAuthority() throws ServletException, IOException {
        authenticateTokenAs(Role.USER);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void shouldAuthenticateAdminWithAdminAuthority() throws ServletException, IOException {
        authenticateTokenAs(Role.ADMIN);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    private void authenticateTokenAs(Role role) throws ServletException, IOException {
        String token = "signed-token";
        UUID userId = UUID.randomUUID();

        when(jwtService.isValid(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(jwtService.extractRole(token)).thenReturn(role);
        when(userRepository.findById(userId)).thenReturn(Optional.of(
                new User("user", "user@example.com", "hash", role)
        ));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        new JwtAuthenticationFilter(jwtService, userRepository).doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain()
        );
    }

    @Test
    void shouldRejectDisabledUserToken() throws ServletException, IOException {
        String token = "signed-token";
        UUID userId = UUID.randomUUID();
        when(jwtService.isValid(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(
                new User("user", "user@example.com", "hash", Role.USER)
        ));

        User disabledUser = userRepository.findById(userId).orElseThrow();
        disabledUser.setEnabled(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(disabledUser));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        new JwtAuthenticationFilter(jwtService, userRepository).doFilter(
                request, new MockHttpServletResponse(), new MockFilterChain()
        );

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();
    }
}
