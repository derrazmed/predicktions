package com.ven.predicktions.repository;

import com.ven.predicktions.model.LeagueMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeagueMemberRepository extends JpaRepository<LeagueMember, UUID> {

    List<LeagueMember> findAllByLeagueId(UUID leagueId);

    List<LeagueMember> findAllByUserId(UUID userId);

    Optional<LeagueMember> findByLeagueIdAndUserId(UUID leagueId, UUID userId);
}
