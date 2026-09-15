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

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            java.time.Instant start,
            java.time.Instant end
    );

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
        SELECT COUNT(p.id) AS totalPredictions,
               COALESCE(SUM(CASE WHEN p.points > 0 THEN 1 ELSE 0 END), 0) AS predictionsWithPoints,
               COALESCE(SUM(p.points), 0) AS totalPointsAwarded,
               COALESCE(SUM(CASE WHEN p.predicted_home_score = m.home_score
                                  AND p.predicted_away_score = m.away_score
                                  THEN 1 ELSE 0 END), 0) AS exactScorePredictions,
               COUNT(DISTINCT p.user_id) AS participatingMembers
        FROM predictions p
        JOIN matches m ON m.id = p.match_id
        JOIN league_members lm ON lm.user_id = p.user_id
        WHERE lm.league_id = :leagueId
        """, nativeQuery = true)
    LeaguePredictionStatistics findLeaguePredictionStatistics(@Param("leagueId") UUID leagueId);

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
        SELECT u.id, u.username, COALESCE(SUM(p.points), 0) AS total_points
        FROM predictions p
        JOIN users u ON u.id = p.user_id
        JOIN matches m ON m.id = p.match_id
        WHERE (:season IS NULL OR EXTRACT(YEAR FROM (m.kickoff_at AT TIME ZONE 'UTC')) = :season)
          AND (:gameweek IS NULL OR EXTRACT(ISOWeek FROM (m.kickoff_at AT TIME ZONE 'UTC')) = :gameweek)
        GROUP BY u.id, u.username
        ORDER BY total_points DESC, u.username ASC
        """, nativeQuery = true)
    List<Object[]> findGlobalLeaderboard(
            @Param("season") Integer season,
            @Param("gameweek") Integer gameweek
    );

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