ALTER TABLE matches
    ADD COLUMN external_id VARCHAR(100) NOT NULL,
    ADD CONSTRAINT uk_matches_external_id UNIQUE (external_id);
