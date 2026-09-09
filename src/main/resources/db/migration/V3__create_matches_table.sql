CREATE TABLE matches (
                        id UUID PRIMARY KEY,
                        home_team VARCHAR(100) NOT NULL,
                        away_team VARCHAR(100) NOT NULL,
                        kickoff_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        home_score INTEGER,
                        away_score INTEGER,
                        status VARCHAR(20) NOT NULL
);
