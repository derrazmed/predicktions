package com.ven.predicktions.repository;

import com.ven.predicktions.model.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PredictionRepository extends JpaRepository<Prediction, UUID> {

    List<Prediction> findAllByUserId(UUID userId);

    Optional<Prediction> findByUserIdAndMatchId(UUID userId, UUID matchId);

    List<Prediction> findAllByMatchId(UUID matchId);
}