package com.ven.predicktions.repository;

import com.ven.predicktions.model.PointsAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PointsAdjustmentRepository extends JpaRepository<PointsAdjustment, UUID> {
}
