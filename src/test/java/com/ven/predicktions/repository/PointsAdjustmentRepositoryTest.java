package com.ven.predicktions.repository;

import com.ven.predicktions.model.PointsAdjustment;
import com.ven.predicktions.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PointsAdjustmentRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private PointsAdjustmentRepository pointsAdjustmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFilterByUserOrderNewestFirstAndPaginate() throws InterruptedException {
        User player = saveUser("player");
        User otherPlayer = saveUser("other-player");

        PointsAdjustment oldest = pointsAdjustmentRepository.saveAndFlush(
                new PointsAdjustment(player, 10, "Oldest", otherPlayer)
        );
        Thread.sleep(2);
        PointsAdjustment newest = pointsAdjustmentRepository.saveAndFlush(
                new PointsAdjustment(player, -5, "Newest", otherPlayer)
        );
        pointsAdjustmentRepository.saveAndFlush(
                new PointsAdjustment(otherPlayer, 20, "Other player", player)
        );

        Page<PointsAdjustment> page =
                pointsAdjustmentRepository.findByUserIdOrderByCreatedAtDescIdDesc(
                        player.getId(),
                        PageRequest.of(0, 1)
                );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).singleElement()
                .extracting(PointsAdjustment::getId)
                .isEqualTo(newest.getId());
        assertThat(oldest.getId()).isNotEqualTo(newest.getId());
    }

    private User saveUser(String username) {
        return userRepository.save(new User(
                username + "-" + System.nanoTime(),
                username + "-" + System.nanoTime() + "@example.com",
                "hashed-password"
        ));
    }
}
