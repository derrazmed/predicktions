ALTER TABLE points_adjustments
    DROP CONSTRAINT chk_points_adjustments_points;

ALTER TABLE points_adjustments
    ADD CONSTRAINT chk_points_adjustments_points
        CHECK (points <> 0);
