CREATE TABLE predictions (
                             id UUID PRIMARY KEY,
                             user_id UUID NOT NULL,
                             match_id UUID NOT NULL,
                             predicted_home_score INTEGER NOT NULL,
                             predicted_away_score INTEGER NOT NULL,
                             points INTEGER NOT NULL DEFAULT 0,
                             created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                             updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                             CONSTRAINT fk_predictions_user
                                 FOREIGN KEY (user_id)
                                     REFERENCES users (id),

                             CONSTRAINT fk_predictions_match
                                 FOREIGN KEY (match_id)
                                     REFERENCES matches (id),

                             CONSTRAINT uk_predictions_user_match
                                 UNIQUE (user_id, match_id),

                             CONSTRAINT chk_predictions_home_score
                                 CHECK (predicted_home_score >= 0),

                             CONSTRAINT chk_predictions_away_score
                                 CHECK (predicted_away_score >= 0),

                             CONSTRAINT chk_predictions_points
                                 CHECK (points >= 0)
);