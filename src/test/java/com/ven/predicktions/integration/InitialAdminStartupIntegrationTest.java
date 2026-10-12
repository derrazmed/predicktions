package com.ven.predicktions.integration;

import com.ven.predicktions.model.Role;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boots the full application with the bootstrap variables configured and proves
 * the initial administrator is created by the startup runner itself.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "admin.initial.name=startup-admin",
                "admin.initial.email=Startup@Example.com",
                "admin.initial.password=Startup-Pass-123"
        }
)
class InitialAdminStartupIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createsInitialAdministratorOnApplicationStartup() {
        User admin = userRepository.findByUsername("startup-admin").orElseThrow();

        assertEquals(Role.ADMIN, admin.getRole());
        assertTrue(admin.isEnabled());
        assertEquals("startup@example.com", admin.getEmail());
        assertTrue(passwordEncoder.matches("Startup-Pass-123", admin.getPasswordHash()));
        assertEquals(1L, userRepository.countByRole(Role.ADMIN));
    }
}
