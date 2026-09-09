package com.ven.predicktions.repository;

import com.ven.predicktions.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldPersistAndFindUserByEmail() {
        User user = new User(
                "testuser",
                "test@example.com",
                "hashed-password"
        );

        userRepository.save(user);

        Optional<User> result = userRepository.findByEmail("test@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        assertThat(result.get().getPasswordHash()).isEqualTo("hashed-password");
    }

    @Test
    void shouldCheckUsernameExistence() {
        User user = new User(
                "testuser",
                "test@example.com",
                "hashed-password"
        );

        userRepository.save(user);

        assertThat(userRepository.existsByUsername("testuser")).isTrue();
        assertThat(userRepository.existsByUsername("unknown")).isFalse();
    }

    @Test
    void shouldCheckEmailExistence() {
        User user = new User(
                "testuser",
                "test@example.com",
                "hashed-password"
        );

        userRepository.save(user);

        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("unknown@example.com")).isFalse();
    }
}