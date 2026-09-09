package com.ven.predicktions.repository;

import com.ven.predicktions.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    Optional<Match> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);
}
