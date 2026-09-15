package com.ven.predicktions.repository;

import com.ven.predicktions.model.PointsAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface PointsAdjustmentRepository extends
        JpaRepository<PointsAdjustment, UUID>,
        JpaSpecificationExecutor<PointsAdjustment> {

    Page<PointsAdjustment> findByUserIdOrderByCreatedAtDescIdDesc(
            UUID userId,
            Pageable pageable
    );
}
