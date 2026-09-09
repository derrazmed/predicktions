package com.ven.predicktions.service;

import com.ven.predicktions.dto.prediction.CreatePredictionRequest;
import com.ven.predicktions.dto.prediction.PredictionResponse;
import com.ven.predicktions.exception.DuplicateResourceException;
import com.ven.predicktions.exception.InvalidPredictionException;
import com.ven.predicktions.exception.PredictionLockedException;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.mapper.PredictionMapper;
import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.MatchStatus;
import com.ven.predicktions.model.Prediction;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.MatchRepository;
import com.ven.predicktions.repository.PredictionRepository;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.impl.PredictionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PredictionServiceImplTest {

    @Mock
    private PredictionRepository predictionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private PredictionMapper predictionMapper;

    private PredictionServiceImpl predictionService;

    private UUID userId;
    private UUID matchId;

    @BeforeEach
    void setUp() {
        predictionService = new PredictionServiceImpl(
                predictionRepository,
                userRepository,
                matchRepository,
                predictionMapper
        );

        userId = UUID.randomUUID();
        matchId = UUID.randomUUID();
    }

    @Test
    void createPrediction_beforeKickoff_success() {
        User user = createUser();
        Match match = createMatch(
                Instant.now().plusSeconds(3600),
                MatchStatus.SCHEDULED
        );

        CreatePredictionRequest request = new CreatePredictionRequest(
                matchId,
                2,
                1
        );

        Prediction prediction = new Prediction(
                user,
                match,
                2,
                1
        );

        PredictionResponse response = new PredictionResponse(
                UUID.randomUUID(),
                matchId,
                2,
                1,
                0,
                Instant.now(),
                Instant.now()
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(matchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(predictionRepository.findByUserIdAndMatchId(userId, matchId))
                .thenReturn(Optional.empty());

        when(predictionRepository.save(any(Prediction.class)))
                .thenReturn(prediction);

        when(predictionMapper.toResponse(prediction))
                .thenReturn(response);

        PredictionResponse result =
                predictionService.createPrediction(userId, request);

        assertThat(result).isEqualTo(response);

        verify(predictionRepository).save(any(Prediction.class));
        verify(predictionMapper).toResponse(prediction);
    }

    @Test
    void createPrediction_atKickoff_rejected() {
        User user = createUser();

        Instant kickoff = Instant.now();

        Match match = createMatch(
                kickoff,
                MatchStatus.SCHEDULED
        );

        CreatePredictionRequest request = new CreatePredictionRequest(
                matchId,
                2,
                1
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(matchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        assertThatThrownBy(() ->
                predictionService.createPrediction(userId, request)
        )
                .isInstanceOf(PredictionLockedException.class)
                .hasMessage("Predictions are locked because the match has already started.");

        verify(predictionRepository, never()).save(any());
    }

    @Test
    void createPrediction_afterKickoff_rejected() {
        User user = createUser();

        Match match = createMatch(
                Instant.now().minusSeconds(3600),
                MatchStatus.SCHEDULED
        );

        CreatePredictionRequest request = new CreatePredictionRequest(
                matchId,
                2,
                1
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(matchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        assertThatThrownBy(() ->
                predictionService.createPrediction(userId, request)
        )
                .isInstanceOf(PredictionLockedException.class)
                .hasMessage("Predictions are locked because the match has already started.");

        verify(predictionRepository, never()).save(any());
    }

    @Test
    void createPrediction_finishedMatch_rejected() {
        User user = createUser();

        Match match = createMatch(
                Instant.now().minusSeconds(3600),
                MatchStatus.FINISHED
        );

        CreatePredictionRequest request = new CreatePredictionRequest(
                matchId,
                2,
                1
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(matchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        assertThatThrownBy(() ->
                predictionService.createPrediction(userId, request)
        )
                .isInstanceOf(InvalidPredictionException.class)
                .hasMessage("Predictions cannot be submitted for a finished match.");

        verify(predictionRepository, never()).save(any());
    }

    @Test
    void createPrediction_cancelledMatch_rejected() {
        User user = createUser();

        Match match = createMatch(
                Instant.now().plusSeconds(3600),
                MatchStatus.CANCELLED
        );

        CreatePredictionRequest request = new CreatePredictionRequest(
                matchId,
                2,
                1
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(matchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        assertThatThrownBy(() ->
                predictionService.createPrediction(userId, request)
        )
                .isInstanceOf(InvalidPredictionException.class)
                .hasMessage("Predictions cannot be submitted for a cancelled match.");

        verify(predictionRepository, never()).save(any());
    }

    @Test
    void createPrediction_duplicate_rejected() {
        User user = createUser();

        Match match = createMatch(
                Instant.now().plusSeconds(3600),
                MatchStatus.SCHEDULED
        );

        Prediction existingPrediction = new Prediction(
                user,
                match,
                2,
                1
        );

        CreatePredictionRequest request = new CreatePredictionRequest(
                matchId,
                3,
                2
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(matchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(predictionRepository.findByUserIdAndMatchId(userId, matchId))
                .thenReturn(Optional.of(existingPrediction));

        assertThatThrownBy(() ->
                predictionService.createPrediction(userId, request)
        )
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Prediction already exists for this match");

        verify(predictionRepository, never()).save(any());
    }

    @Test
    void createPrediction_nonExistentMatch_rejected() {
        User user = createUser();

        CreatePredictionRequest request = new CreatePredictionRequest(
                matchId,
                2,
                1
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(matchRepository.findById(matchId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                predictionService.createPrediction(userId, request)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Match not found");

        verify(predictionRepository, never()).save(any());
    }

    private User createUser() {
        return new User(
                "testuser-" + UUID.randomUUID(),
                "test-" + UUID.randomUUID() + "@example.com",
                "password"
        );
    }

    private Match createMatch(
            Instant kickoffAt,
            MatchStatus status
    ) {
        return new Match(
                "match-" + UUID.randomUUID(),
                "Home FC",
                "Away FC",
                kickoffAt,
                status
        );
    }
}