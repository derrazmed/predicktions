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

    Optional<Prediction> findByUserIdAndMatchId(UUID userId, UUID matchId);

    List<Prediction> findAllByMatchId(UUID matchId);

    @Query("""
        SELECT p.user.id, p.user.username, COALESCE(SUM(p.points), 0)
        FROM Prediction p
        GROUP BY p.user.id, p.user.username
        ORDER BY COALESCE(SUM(p.points), 0) DESC, p.user.username ASC
    """)
    List<Object[]> findGlobalLeaderboard();

    @Query("""
        SELECT
            u.id,
            u.username,
            COALESCE(SUM(p.points), 0)
        FROM LeagueMember lm
        JOIN lm.user u
        LEFT JOIN Prediction p ON p.user.id = u.id
        WHERE lm.league.id = :leagueId
        GROUP BY u.id, u.username
        ORDER BY COALESCE(SUM(p.points), 0) DESC
    """)
    List<Object[]> findLeagueLeaderboard(@Param("leagueId") UUID leagueId);
}