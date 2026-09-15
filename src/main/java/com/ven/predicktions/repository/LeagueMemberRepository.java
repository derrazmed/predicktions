package com.ven.predicktions.repository;

import com.ven.predicktions.model.LeagueMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeagueMemberRepository extends JpaRepository<LeagueMember, UUID> {

    List<LeagueMember> findAllByLeagueId(UUID leagueId);

    @Query("SELECT m FROM LeagueMember m JOIN FETCH m.user WHERE m.league.id = :leagueId ORDER BY m.createdAt ASC, m.id ASC")
    List<LeagueMember> findAllByLeagueIdOrdered(@Param("leagueId") UUID leagueId);

    List<LeagueMember> findAllByUserId(UUID userId);

    Optional<LeagueMember> findByLeagueIdAndUserId(UUID leagueId, UUID userId);

    boolean existsByLeagueIdAndUserId(UUID leagueId, UUID userId);

    long countByLeagueId(UUID leagueId);
}
