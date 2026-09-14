package com.ven.predicktions.service;

import com.ven.predicktions.dto.user.AdminUserPageResponse;
import com.ven.predicktions.dto.user.AdminUserDetailsResponse;
import com.ven.predicktions.dto.user.AdminUserResponse;
import com.ven.predicktions.dto.leaderboard.LeaderboardEntryResponse;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.ven.predicktions.repository.PredictionRepository predictionRepository;

    @Mock
    private com.ven.predicktions.repository.LeagueMemberRepository leagueMemberRepository;

    @Mock
    private LeaderboardService leaderboardService;

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
                new AdminUserServiceImpl(
                        userRepository,
                        predictionRepository,
                        leagueMemberRepository,
                        leaderboardService
                ).getUsers(0, 20);

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
                () -> new AdminUserServiceImpl(
                        userRepository,
                        predictionRepository,
                        leagueMemberRepository,
                        leaderboardService
                ).getUsers(0, 101)
        );
    }

    @Test
    void shouldBuildUserDetailsFromExistingStatistics() {
        UUID userId = UUID.randomUUID();
        User user = new User(
                "testuser",
                "test@example.com",
                "secret-hash",
                Role.USER
        );
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(predictionRepository.countByUserId(userId)).thenReturn(42L);
        when(predictionRepository.sumPointsByUserId(userId)).thenReturn(127L);
        when(leagueMemberRepository.findAllByUserId(userId)).thenReturn(List.of());
        when(leaderboardService.getGlobalLeaderboard()).thenReturn(List.of(
                new LeaderboardEntryResponse(8, userId, "testuser", 127)
        ));

        AdminUserDetailsResponse response = new AdminUserServiceImpl(
                userRepository,
                predictionRepository,
                leagueMemberRepository,
                leaderboardService
        ).getUser(userId);

        assertThat(response.predictionCount()).isEqualTo(42);
        assertThat(response.points()).isEqualTo(127);
        assertThat(response.leaderboardPosition()).isEqualTo(8);
        assertThat(response.leagues()).isEmpty();
    }

    @Test
    void shouldPromoteUserToAdmin() {
        UUID userId = UUID.randomUUID();
        User user = new User("user", "user@example.com", "hash", Role.USER);
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        AdminUserResponse response = service().changeUserRole(userId, Role.ADMIN);

        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(response.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void shouldDemoteAdminWhenAnotherAdminExists() {
        UUID userId = UUID.randomUUID();
        User user = new User("admin", "admin@example.com", "hash", Role.ADMIN);
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(userRepository.findAllByRoleForUpdate(Role.ADMIN))
                .thenReturn(List.of(user, new User("other", "other@example.com", "hash", Role.ADMIN)));

        service().changeUserRole(userId, Role.USER);

        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void shouldRejectDemotionOfLastAdmin() {
        UUID userId = UUID.randomUUID();
        User user = new User("admin", "admin@example.com", "hash", Role.ADMIN);
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(userRepository.findAllByRoleForUpdate(Role.ADMIN)).thenReturn(List.of(user));

        assertThrows(
                com.ven.predicktions.exception.LastAdministratorException.class,
                () -> service().changeUserRole(userId, Role.USER)
        );

        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void shouldTreatSameRoleAsNoOp() {
        UUID userId = UUID.randomUUID();
        User user = new User("admin", "admin@example.com", "hash", Role.ADMIN);
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        service().changeUserRole(userId, Role.ADMIN);

        verify(userRepository, org.mockito.Mockito.never())
                .findAllByRoleForUpdate(Role.ADMIN);
    }

    private AdminUserServiceImpl service() {
        return new AdminUserServiceImpl(
                userRepository,
                predictionRepository,
                leagueMemberRepository,
                leaderboardService
        );
    }
}
