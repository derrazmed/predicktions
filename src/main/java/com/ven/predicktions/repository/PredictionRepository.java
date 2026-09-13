package com.ven.predicktions.repository;

import com.ven.predicktions.model.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PredictionRepository extends JpaRepository<Prediction, UUID> {

    List<Prediction> findAllByUserId(UUID userId);

    Optional<Prediction> findByUserIdAndMatchId(UUID userId, UUID matchId);

    List<Prediction> findAllByMatchId(UUID matchId);

    @Query("""
        SELECT p.user.id, p.user.username, COALESCE(SUM(p.points), 0)
        FROM Prediction p
        GROUP BY p.user.id, p.user.username
        ORDER BY COALESCE(SUM(p.points), 0) DESC, p.user.username ASC
    """)
    List<Object[]> findGlobalLeaderboard();
}