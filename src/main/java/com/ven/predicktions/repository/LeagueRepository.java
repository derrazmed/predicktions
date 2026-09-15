package com.ven.predicktions.repository;

import com.ven.predicktions.model.League;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface LeagueRepository extends JpaRepository<League, UUID> {

    @Query(value = """
        SELECT l.id AS id,
               l.name AS name,
               o.id AS ownerId,
               o.username AS ownerUsername,
               COUNT(m.id) AS memberCount,
               l.createdAt AS createdAt,
               l.joinCode AS joinCode
        FROM League l
        JOIN l.owner o
        LEFT JOIN l.members m
        GROUP BY l.id, l.name, o.id, o.username, l.createdAt, l.joinCode
        ORDER BY l.createdAt DESC, l.id DESC
        """,
        countQuery = "SELECT COUNT(l) FROM League l")
    Page<AdminLeagueProjection> findAllForAdministration(Pageable pageable);

    Optional<League> findByJoinCode(String joinCode);

    boolean existsByJoinCode(String joinCode);
}
