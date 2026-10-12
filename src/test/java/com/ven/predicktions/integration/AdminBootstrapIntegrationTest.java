package com.ven.predicktions.integration;

import com.ven.predicktions.model.Role;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.AdminBootstrapService;
import com.ven.predicktions.service.InitialAdminOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end (real PostgreSQL) coverage for the initial administrator
 * bootstrap service. A dedicated container/context keeps this isolated from the
 * rest of the suite.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminBootstrapIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private AdminBootstrapService adminBootstrapService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void createsAndHashesInitialAdministrator() {
        InitialAdminOutcome outcome =
                adminBootstrapService.createInitialAdmin("root-admin", "Root@Example.com", "S3cret-Pass!");

        assertEquals(InitialAdminOutcome.CREATED, outcome);

        User admin = userRepository.findByUsername("root-admin").orElseThrow();
        assertEquals(Role.ADMIN, admin.getRole());
        assertTrue(admin.isEnabled());
        assertEquals("root@example.com", admin.getEmail());
        assertNotEquals("S3cret-Pass!", admin.getPasswordHash());
        assertTrue(passwordEncoder.matches("S3cret-Pass!", admin.getPasswordHash()));
        assertEquals(1L, userRepository.countByRole(Role.ADMIN));
    }

    @Test
    void isIdempotentWhenRunAgain() {
        assertEquals(InitialAdminOutcome.CREATED,
                adminBootstrapService.createInitialAdmin("root-admin", "root@example.com", "first"));

        assertEquals(InitialAdminOutcome.SKIPPED_ADMIN_EXISTS,
                adminBootstrapService.createInitialAdmin("root-admin", "root@example.com", "second"));

        assertEquals(1L, userRepository.countByRole(Role.ADMIN));
        assertEquals(1L, userRepository.count());
    }

    @Test
    void doesNotModifyExistingAdministrator() {
        adminBootstrapService.createInitialAdmin("root-admin", "root@example.com", "original");
        String originalHash =
                userRepository.findByUsername("root-admin").orElseThrow().getPasswordHash();

        assertEquals(InitialAdminOutcome.SKIPPED_ADMIN_EXISTS,
                adminBootstrapService.createInitialAdmin("root-admin", "root@example.com", "changed"));

        User reloaded = userRepository.findByUsername("root-admin").orElseThrow();
        assertEquals(originalHash, reloaded.getPasswordHash());
        assertTrue(passwordEncoder.matches("original", reloaded.getPasswordHash()));
    }

    @Test
    void doesNotPromoteExistingRegularUserWithSameEmail() {
        userRepository.save(new User("someone", "taken@example.com", passwordEncoder.encode("pw")));

        InitialAdminOutcome outcome =
                adminBootstrapService.createInitialAdmin("root-admin", "Taken@Example.com", "pw");

        assertEquals(InitialAdminOutcome.SKIPPED_EMAIL_IN_USE, outcome);
        assertEquals(Role.USER, userRepository.findByUsername("someone").orElseThrow().getRole());
        assertEquals(0L, userRepository.countByRole(Role.ADMIN));
    }

    @Test
    void doesNotCreateAdministratorWhenUsernameIsTaken() {
        userRepository.save(new User("root-admin", "other@example.com", passwordEncoder.encode("pw")));

        InitialAdminOutcome outcome =
                adminBootstrapService.createInitialAdmin("root-admin", "root@example.com", "pw");

        assertEquals(InitialAdminOutcome.SKIPPED_USERNAME_IN_USE, outcome);
        assertEquals(0L, userRepository.countByRole(Role.ADMIN));
    }
}
