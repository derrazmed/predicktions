package com.ven.predicktions.repository;

import com.ven.predicktions.model.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PredictionRepository extends JpaRepository<Prediction, UUID>,
        JpaSpecificationExecutor<Prediction> {

    List<Prediction> findAllByUserId(UUID userId);

    long countByUserId(UUID userId);

    @Query("""
        SELECT COALESCE(SUM(p.points), 0)
        FROM Prediction p
        WHERE p.user.id = :userId
    """)
    long sumPointsByUserId(@Param("userId") UUID userId);

    @Query(value = """
        SELECT COALESCE((SELECT SUM(p.points) FROM predictions p WHERE p.user_id = :userId), 0)
             + COALESCE((SELECT SUM(a.points) FROM points_adjustments a WHERE a.user_id = :userId), 0)
        """, nativeQuery = true)
    long sumTotalPointsByUserId(@Param("userId") UUID userId);

    Optional<Prediction> findByUserIdAndMatchId(UUID userId, UUID matchId);

    List<Prediction> findAllByMatchId(UUID matchId);

    @Query(value = """
        SELECT u.id, u.username,
               COALESCE((SELECT SUM(p.points) FROM predictions p WHERE p.user_id = u.id), 0)
               + COALESCE((SELECT SUM(a.points) FROM points_adjustments a WHERE a.user_id = u.id), 0)
               AS total_points
        FROM users u
        ORDER BY total_points DESC, u.username ASC
        """, nativeQuery = true)
    List<Object[]> findGlobalLeaderboard();

    @Query(value = """
        SELECT u.id, u.username,
               COALESCE((SELECT SUM(p.points) FROM predictions p WHERE p.user_id = u.id), 0)
               + COALESCE((SELECT SUM(a.points) FROM points_adjustments a WHERE a.user_id = u.id), 0)
               AS total_points
        FROM league_members lm
        JOIN users u ON u.id = lm.user_id
        WHERE lm.league_id = :leagueId
        ORDER BY total_points DESC, u.username ASC
        """, nativeQuery = true)
    List<Object[]> findLeagueLeaderboard(@Param("leagueId") UUID leagueId);
}