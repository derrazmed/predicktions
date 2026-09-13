package com.ven.predicktions.service;

import com.ven.predicktions.dto.auth.RegisterRequest;
import com.ven.predicktions.dto.auth.RegisterResponse;
import com.ven.predicktions.exception.DuplicateResourceException;
import com.ven.predicktions.model.User;
import com.ven.predicktions.model.Role;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.security.JwtService;
import com.ven.predicktions.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.ven.predicktions.dto.auth.LoginRequest;
import com.ven.predicktions.dto.auth.LoginResponse;
import org.springframework.security.authentication.BadCredentialsException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @Mock
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void shouldRegisterUser() {
        RegisterRequest request = new RegisterRequest(
                "derrazz",
                "user@example.com",
                "password123"
        );

        User savedUser = new User(
                "derrazz",
                "user@example.com",
                "hashed-password"
        );

        when(userRepository.existsByUsername("derrazz"))
                .thenReturn(false);

        when(userRepository.existsByEmail("user@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("hashed-password");

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        RegisterResponse response = authService.register(request);

        assertEquals("derrazz", response.username());
        assertEquals("user@example.com", response.email());
        assertEquals(Role.USER, response.role());

        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(argThat(user ->
                user.getRole() == Role.USER
        ));
    }

    @Test
    void shouldRejectDuplicateUsername() {
        RegisterRequest request = new RegisterRequest(
                "derrazz",
                "user@example.com",
                "password123"
        );

        when(userRepository.existsByUsername("derrazz"))
                .thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> authService.register(request)
        );

        assertEquals("Username is already in use", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "derrazz",
                "user@example.com",
                "password123"
        );

        when(userRepository.existsByUsername("derrazz"))
                .thenReturn(false);

        when(userRepository.existsByEmail("user@example.com"))
                .thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> authService.register(request)
        );

        assertEquals("Email is already in use", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldHashPasswordBeforePersistence() {
        RegisterRequest request = new RegisterRequest(
                "derrazz",
                "user@example.com",
                "password123"
        );

        when(userRepository.existsByUsername("derrazz"))
                .thenReturn(false);

        when(userRepository.existsByEmail("user@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("hashed-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

        verify(userRepository).save(argThat(user ->
                user.getPasswordHash().equals("hashed-password")
                        && !user.getPasswordHash().equals("password123")
        ));
    }

    @Test
    void shouldLoginUser() {
        LoginRequest request = new LoginRequest(
                "derrazz",
                "password123"
        );

        User user = new User(
                "derrazz",
                "user@example.com",
                "hashed-password"
        );

        String token = "jwt-token";

        when(userRepository.findByUsername("derrazz"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password123", "hashed-password"))
                .thenReturn(true);

        when(jwtService.generateToken(user.getId(), Role.USER))
                .thenReturn(token);

        when(jwtService.getExpiration())
                .thenReturn(3600000L);

        LoginResponse response = authService.login(request);

        assertEquals(token, response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600, response.expiresIn());

        verify(userRepository).findByUsername("derrazz");
        verify(passwordEncoder).matches("password123", "hashed-password");
        verify(jwtService).generateToken(user.getId(), Role.USER);
    }

    @Test
    void shouldRejectInvalidPassword() {
        LoginRequest request = new LoginRequest(
                "derrazz",
                "wrong-password"
        );

        User user = new User(
                "derrazz",
                "user@example.com",
                "hashed-password"
        );

        when(userRepository.findByUsername("derrazz"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("wrong-password", "hashed-password"))
                .thenReturn(false);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        assertEquals(
                "Invalid username or password",
                exception.getMessage()
        );

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void shouldRejectUnknownUsername() {
        LoginRequest request = new LoginRequest(
                "unknown",
                "password123"
        );

        when(userRepository.findByUsername("unknown"))
                .thenReturn(Optional.empty());

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        assertEquals(
                "Invalid username or password",
                exception.getMessage()
        );

        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any());
    }
}