package com.ven.predicktions.service;

import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.repository.PredictionRepository;
import com.ven.predicktions.repository.MatchRepository;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.impl.AdminPredictionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPredictionServiceTest {

    @Mock
    private PredictionRepository predictionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MatchRepository matchRepository;

    @Test
    void shouldRejectPredictionHistoryForUnknownUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(false);

        AdminPredictionService service =
                new AdminPredictionServiceImpl(
                        predictionRepository, userRepository, matchRepository
                );

        assertThatThrownBy(() -> service.getUserPredictions(userId, 0, 20))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userRepository).existsById(userId);
    }

    @Test
    void shouldRejectPredictionsForUnknownMatch() {
        UUID matchId = UUID.randomUUID();
        when(matchRepository.existsById(matchId)).thenReturn(false);

        AdminPredictionService service =
                new AdminPredictionServiceImpl(
                        predictionRepository, userRepository, matchRepository
                );

        assertThatThrownBy(() -> service.getMatchPredictions(matchId, 0, 20))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(matchRepository).existsById(matchId);
    }
}
