package com.ven.predicktions.service;

import com.ven.predicktions.dto.user.AdminUserPageResponse;
import com.ven.predicktions.model.Role;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void shouldReturnSafePaginatedUserResponses() {
        User user = new User(
                "testuser",
                "test@example.com",
                "secret-hash",
                Role.USER
        );
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        AdminUserPageResponse response =
                new AdminUserServiceImpl(userRepository).getUsers(0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().username()).isEqualTo("testuser");
        assertThat(response.content().getFirst().email()).isEqualTo("test@example.com");
        assertThat(response.content().getFirst().role()).isEqualTo(Role.USER);
        assertThat(response.content().getFirst().enabled()).isTrue();
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void shouldRejectPageSizeAboveLimit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AdminUserServiceImpl(userRepository).getUsers(0, 101)
        );
    }
}
