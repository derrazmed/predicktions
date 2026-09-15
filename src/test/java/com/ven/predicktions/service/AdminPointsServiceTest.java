package com.ven.predicktions.service;

import com.ven.predicktions.dto.user.PointsAdjustmentRequest;
import com.ven.predicktions.dto.user.PointsAdjustmentResponse;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.model.PointsAdjustment;
import com.ven.predicktions.model.Role;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.PointsAdjustmentRepository;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.impl.AdminPointsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPointsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointsAdjustmentRepository pointsAdjustmentRepository;

    @Test
    void shouldPersistAuditablePositiveAward() {
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        User user = new User("player", "player@example.com", "hash");
        User admin = new User("admin", "admin@example.com", "hash", Role.ADMIN);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(pointsAdjustmentRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PointsAdjustmentResponse response = service().adjustPoints(
                userId,
                new PointsAdjustmentRequest(10, "Correction"),
                adminId
        );

        ArgumentCaptor<PointsAdjustment> captor =
                ArgumentCaptor.forClass(PointsAdjustment.class);
        verify(pointsAdjustmentRepository).save(captor.capture());
        PointsAdjustment adjustment = captor.getValue();
        assertThat(adjustment.getUser()).isSameAs(user);
        assertThat(adjustment.getPoints()).isEqualTo(10);
        assertThat(adjustment.getReason()).isEqualTo("Correction");
        assertThat(adjustment.getAdjustedBy()).isSameAs(admin);
        assertThat(response.username()).isEqualTo("player");
        assertThat(response.points()).isEqualTo(10);
    }

    @Test
    void shouldRejectUnknownTargetWithoutPersistingAdjustment() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service().adjustPoints(
                        userId,
                        new PointsAdjustmentRequest(10, "Correction"),
                        UUID.randomUUID()
                )
        );

        org.mockito.Mockito.verifyNoInteractions(pointsAdjustmentRepository);
    }

    @Test
    void shouldPersistNegativeAdjustmentWithoutChangingItsSign() {
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        User user = new User("player", "player@example.com", "hash");
        User admin = new User("admin", "admin@example.com", "hash", Role.ADMIN);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(pointsAdjustmentRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PointsAdjustmentResponse response = service().adjustPoints(
                userId,
                new PointsAdjustmentRequest(-5, "Manual correction"),
                adminId
        );

        ArgumentCaptor<PointsAdjustment> captor =
                ArgumentCaptor.forClass(PointsAdjustment.class);
        verify(pointsAdjustmentRepository).save(captor.capture());
        assertThat(captor.getValue().getPoints()).isEqualTo(-5);
        assertThat(response.points()).isEqualTo(-5);
    }

    private AdminPointsServiceImpl service() {
        return new AdminPointsServiceImpl(userRepository, pointsAdjustmentRepository);
    }
}
