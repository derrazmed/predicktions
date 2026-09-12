CREATE TABLE leagues (
                         id UUID PRIMARY KEY,
                         name VARCHAR(100) NOT NULL,
                         owner_id UUID NOT NULL,
                         join_code VARCHAR(32) NOT NULL,
                         created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                         CONSTRAINT fk_leagues_owner
                             FOREIGN KEY (owner_id)
                                 REFERENCES users (id),

                         CONSTRAINT uk_leagues_join_code UNIQUE (join_code)
);

CREATE TABLE league_members (
                                id UUID PRIMARY KEY,
                                league_id UUID NOT NULL,
                                user_id UUID NOT NULL,
                                created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                CONSTRAINT fk_league_members_league
                                    FOREIGN KEY (league_id)
                                        REFERENCES leagues (id),

                                CONSTRAINT fk_league_members_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users (id),

                                CONSTRAINT uk_league_members_league_user
                                    UNIQUE (league_id, user_id)
);
