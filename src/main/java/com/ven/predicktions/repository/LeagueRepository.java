package com.ven.predicktions.repository;

import com.ven.predicktions.model.League;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LeagueRepository extends JpaRepository<League, UUID> {

    Optional<League> findByJoinCode(String joinCode);

    boolean existsByJoinCode(String joinCode);
}
