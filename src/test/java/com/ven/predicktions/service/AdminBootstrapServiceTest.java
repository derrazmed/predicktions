package com.ven.predicktions.service;

import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.model.Role;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.impl.AdminBootstrapServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceTest {

    @Mock
    private UserRepository userRepository;

    private AdminBootstrapService adminBootstrapService;

    @BeforeEach
    void setUp() {
        adminBootstrapService = new AdminBootstrapServiceImpl(userRepository);
    }

    @Test
    void shouldPromoteExistingUserByEmail() {
        User user = new User(
                "admin",
                "admin@example.com",
                "hashed-password"
        );
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(user));

        adminBootstrapService.promoteByEmail("admin@example.com");

        assertEquals(Role.ADMIN, user.getRole());
        verify(userRepository).findByEmail("admin@example.com");
    }

    @Test
    void shouldBeIdempotentForExistingAdmin() {
        User user = new User(
                "admin",
                "admin@example.com",
                "hashed-password",
                Role.ADMIN
        );
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(user));

        adminBootstrapService.promoteByEmail("admin@example.com");
        adminBootstrapService.promoteByEmail("admin@example.com");

        assertEquals(Role.ADMIN, user.getRole());
        verify(userRepository, times(2)).findByEmail("admin@example.com");
    }

    @Test
    void shouldRejectUnknownEmailWithoutCreatingUser() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> adminBootstrapService.promoteByEmail("missing@example.com")
        );

        verify(userRepository, never()).save(any(User.class));
    }
}
