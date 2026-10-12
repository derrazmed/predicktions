package com.ven.predicktions.service;

import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.model.Role;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.impl.AdminBootstrapServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminBootstrapService adminBootstrapService;

    @BeforeEach
    void setUp() {
        adminBootstrapService = new AdminBootstrapServiceImpl(userRepository, passwordEncoder);
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
                "hashed-password"
        );
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(user));

        adminBootstrapService.promoteByEmail("admin@example.com");

        assertEquals(Role.ADMIN, user.getRole());
        verify(userRepository).findByEmail("admin@example.com");
    }

    @Test
    void shouldFailPromotionWhenNoMatchingUser() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> adminBootstrapService.promoteByEmail("missing@example.com")
        );
        verify(userRepository, never()).save(any());
    }

    // --- createInitialAdmin -------------------------------------------------

    @Test
    void shouldCreateInitialAdminWhenNoneExists() {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(userRepository.findByEmailIgnoreCase("root@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("root-admin")).thenReturn(false);
        when(passwordEncoder.encode("S3cret-Pass!")).thenReturn("bcrypt-hash");

        InitialAdminOutcome outcome =
                adminBootstrapService.createInitialAdmin("root-admin", "Root@Example.com", "S3cret-Pass!");

        assertEquals(InitialAdminOutcome.CREATED, outcome);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertEquals("root-admin", saved.getUsername());
        assertEquals("root@example.com", saved.getEmail());
        assertEquals(Role.ADMIN, saved.getRole());
        assertTrue(saved.isEnabled());
        assertEquals("bcrypt-hash", saved.getPasswordHash());
    }

    @Test
    void shouldEncodePasswordBeforeStoringIt() {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(userRepository.findByEmailIgnoreCase("root@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("root-admin")).thenReturn(false);
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");

        adminBootstrapService.createInitialAdmin("root-admin", "root@example.com", "raw-password");

        verify(passwordEncoder).encode("raw-password");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertEquals("encoded-password", captor.getValue().getPasswordHash());
    }

    @Test
    void shouldNotCreateAdminWhenConfigurationIsBlank() {
        assertEquals(InitialAdminOutcome.SKIPPED_NOT_CONFIGURED,
                adminBootstrapService.createInitialAdmin("  ", "root@example.com", "pw"));
        assertEquals(InitialAdminOutcome.SKIPPED_NOT_CONFIGURED,
                adminBootstrapService.createInitialAdmin("root-admin", "", "pw"));
        assertEquals(InitialAdminOutcome.SKIPPED_NOT_CONFIGURED,
                adminBootstrapService.createInitialAdmin("root-admin", "root@example.com", " "));
        assertEquals(InitialAdminOutcome.SKIPPED_NOT_CONFIGURED,
                adminBootstrapService.createInitialAdmin(null, null, null));

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void shouldNotCreateAdminWhenOneAlreadyExists() {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        InitialAdminOutcome outcome =
                adminBootstrapService.createInitialAdmin("root-admin", "root@example.com", "pw");

        assertEquals(InitialAdminOutcome.SKIPPED_ADMIN_EXISTS, outcome);
        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldNotPromoteExistingRegularUserWithSameEmail() {
        User regular = new User("someone", "taken@example.com", "hashed");
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(userRepository.findByEmailIgnoreCase("taken@example.com")).thenReturn(Optional.of(regular));

        InitialAdminOutcome outcome =
                adminBootstrapService.createInitialAdmin("root-admin", "Taken@Example.com", "pw");

        assertEquals(InitialAdminOutcome.SKIPPED_EMAIL_IN_USE, outcome);
        assertEquals(Role.USER, regular.getRole());
        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldNotCreateAdminWhenUsernameIsTaken() {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(userRepository.findByEmailIgnoreCase("root@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("root-admin")).thenReturn(true);

        InitialAdminOutcome outcome =
                adminBootstrapService.createInitialAdmin("root-admin", "root@example.com", "pw");

        assertEquals(InitialAdminOutcome.SKIPPED_USERNAME_IN_USE, outcome);
        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldReportConflictOnUniqueViolation() {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(userRepository.findByEmailIgnoreCase("root@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("root-admin")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hash");
        when(userRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        InitialAdminOutcome outcome =
                adminBootstrapService.createInitialAdmin("root-admin", "root@example.com", "pw");

        assertEquals(InitialAdminOutcome.SKIPPED_CONFLICT, outcome);
    }

    @Test
    void shouldReportFailureWithoutThrowingWhenDatabaseFails() {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(userRepository.findByEmailIgnoreCase("root@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("root-admin")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hash");
        when(userRepository.saveAndFlush(any()))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        InitialAdminOutcome outcome =
                adminBootstrapService.createInitialAdmin("root-admin", "root@example.com", "pw");

        assertEquals(InitialAdminOutcome.FAILED, outcome);
    }
}
