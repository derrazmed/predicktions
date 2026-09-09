package com.ven.predicktions.service;

import com.ven.predicktions.dto.user.UserResponse;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository);
    }

    @Test
    void shouldReturnCurrentUser() {
        UUID userId = UUID.randomUUID();

        User user = new User(
                "derrazz",
                "user@example.com",
                "hashed-password"
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        UserResponse response = userService.getCurrentUser(userId);

        assertEquals(user.getId(), response.id());
        assertEquals("derrazz", response.username());
        assertEquals("user@example.com", response.email());

        verify(userRepository).findById(userId);
    }

    @Test
    void shouldRejectUnknownUser() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getCurrentUser(userId)
        );

        assertEquals("User not found", exception.getMessage());

        verify(userRepository).findById(userId);
    }

    @Test
    void shouldNotExposePasswordHash() {
        UUID userId = UUID.randomUUID();

        User user = new User(
                "derrazz",
                "user@example.com",
                "hashed-password"
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        UserResponse response = userService.getCurrentUser(userId);

        assertEquals("derrazz", response.username());
        assertEquals("user@example.com", response.email());

        assertFalse(
                response.getClass().toString().contains("password")
        );
    }
}