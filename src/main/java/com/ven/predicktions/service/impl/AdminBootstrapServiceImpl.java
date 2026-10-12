package com.ven.predicktions.service.impl;

import com.ven.predicktions.model.Role;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.AdminBootstrapService;
import com.ven.predicktions.service.InitialAdminOutcome;
import com.ven.predicktions.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class AdminBootstrapServiceImpl implements AdminBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void promoteByEmail(String email) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cannot bootstrap admin: user not found for email"
                ))
                .promoteToAdmin();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The write runs in the repository's own transaction (this method is
     * intentionally not transactional) so that a unique-constraint violation
     * can be caught and reported without leaving a rollback-only transaction
     * behind. The {@code users} table's unique constraints on {@code username}
     * and {@code email} are the authority when two instances start at once.</p>
     */
    @Override
    public InitialAdminOutcome createInitialAdmin(String name, String email, String rawPassword) {
        if (isBlank(name) || isBlank(email) || isBlank(rawPassword)) {
            log.warn("Initial administrator is not fully configured. Set ADMIN_INITIAL_NAME, "
                    + "ADMIN_INITIAL_EMAIL and ADMIN_INITIAL_PASSWORD to create one on startup. "
                    + "Skipping.");
            return InitialAdminOutcome.SKIPPED_NOT_CONFIGURED;
        }

        String username = name.trim();
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (userRepository.countByRole(Role.ADMIN) > 0) {
            log.info("An administrator already exists; skipping initial administrator creation.");
            return InitialAdminOutcome.SKIPPED_ADMIN_EXISTS;
        }

        Optional<User> existing = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (existing.isPresent()) {
            log.warn("The configured initial administrator email already belongs to an existing "
                    + "account. Refusing to promote it automatically; choose a different email or "
                    + "remove the ADMIN_INITIAL_* variables.");
            return InitialAdminOutcome.SKIPPED_EMAIL_IN_USE;
        }

        if (userRepository.existsByUsername(username)) {
            log.warn("The configured initial administrator name is already in use. Refusing to "
                    + "create the administrator; choose a different name or remove the "
                    + "ADMIN_INITIAL_* variables.");
            return InitialAdminOutcome.SKIPPED_USERNAME_IN_USE;
        }

        String passwordHash = passwordEncoder.encode(rawPassword);

        try {
            userRepository.saveAndFlush(new User(username, normalizedEmail, passwordHash, Role.ADMIN));
        } catch (DataIntegrityViolationException exception) {
            // Another instance (or a concurrent registration) inserted the same
            // username/email first; the unique constraints keep the data consistent.
            log.warn("Initial administrator was not created because the name or email is already "
                    + "in use.");
            return InitialAdminOutcome.SKIPPED_CONFLICT;
        } catch (RuntimeException exception) {
            // Never log the raw password or a full SQL dump; only the failure kind.
            log.error("Failed to create the initial administrator account ({}). The application "
                    + "will continue to start.", exception.getClass().getSimpleName());
            return InitialAdminOutcome.FAILED;
        }

        log.info("Initial administrator account '{}' created.", username);
        return InitialAdminOutcome.CREATED;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
