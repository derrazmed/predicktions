CREATE INDEX idx_points_adjustments_admin_id
    ON points_adjustments (awarded_by);

CREATE INDEX idx_points_adjustments_user_created_at
    ON points_adjustments (user_id, created_at DESC);

CREATE INDEX idx_points_adjustments_admin_created_at
    ON points_adjustments (awarded_by, created_at DESC);
