package com.ven.predicktions.repository;

import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.MatchStatus;
import com.ven.predicktions.model.Prediction;
import com.ven.predicktions.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PredictionRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Test
    void shouldSaveAndFindPredictionById() {
        User user = createUser();
        Match match = createMatch();

        Prediction prediction = new Prediction(
                user,
                match,
                2,
                1
        );

        Prediction saved = predictionRepository.save(prediction);

        Optional<Prediction> result =
                predictionRepository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getPredictedHomeScore()).isEqualTo(2);
        assertThat(result.get().getPredictedAwayScore()).isEqualTo(1);
    }

    @Test
    void shouldFindPredictionsByUser() {
        User user = createUser();

        Match match1 = createMatch("match-1");
        Match match2 = createMatch("match-2");

        predictionRepository.save(
                new Prediction(user, match1, 2, 1)
        );

        predictionRepository.save(
                new Prediction(user, match2, 1, 1)
        );

        List<Prediction> predictions =
                predictionRepository.findAllByUserId(user.getId());

        assertThat(predictions).hasSize(2);
    }

    @Test
    void shouldFindPredictionByUserAndMatch() {
        User user = createUser();
        Match match = createMatch();

        Prediction saved = predictionRepository.save(
                new Prediction(user, match, 2, 1)
        );

        Optional<Prediction> result =
                predictionRepository.findByUserIdAndMatchId(
                        user.getId(),
                        match.getId()
                );

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void shouldFindPredictionsByMatch() {
        User user1 = createUser("user-1");
        User user2 = createUser("user-2");

        Match match = createMatch();

        predictionRepository.save(
                new Prediction(user1, match, 2, 1)
        );

        predictionRepository.save(
                new Prediction(user2, match, 1, 0)
        );

        List<Prediction> predictions =
                predictionRepository.findAllByMatchId(match.getId());

        assertThat(predictions).hasSize(2);
    }

    private User createUser() {
        return createUser("user-" + System.nanoTime());
    }

    private User createUser(String username) {
        User user = new User(
                username,
                username + "@example.com",
                "hashed-password"
        );

        return userRepository.save(user);
    }

    private Match createMatch() {
        return createMatch("match-" + UUID.randomUUID());
    }

    private Match createMatch(String externalId) {
        Match match = new Match(
                externalId,
                "Home FC",
                "Away FC",
                Instant.now().plusSeconds(3600),
                MatchStatus.SCHEDULED
        );

        return matchRepository.save(match);
    }
}