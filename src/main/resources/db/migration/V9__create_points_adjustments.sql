CREATE TABLE points_adjustments (
                                   id UUID PRIMARY KEY,
                                   user_id UUID NOT NULL,
                                   points INTEGER NOT NULL,
                                   reason VARCHAR(500) NOT NULL,
                                   awarded_by UUID NOT NULL,
                                   created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                   CONSTRAINT fk_points_adjustments_user
                                       FOREIGN KEY (user_id)
                                           REFERENCES users (id),

                                   CONSTRAINT fk_points_adjustments_awarded_by
                                       FOREIGN KEY (awarded_by)
                                           REFERENCES users (id),

                                   CONSTRAINT chk_points_adjustments_points
                                       CHECK (points > 0)
);

CREATE INDEX idx_points_adjustments_user_id
    ON points_adjustments (user_id);

CREATE INDEX idx_points_adjustments_created_at
    ON points_adjustments (created_at);
