-- Persist the competition / season / matchday identity supplied by the sports
-- provider so a "gameweek" is the provider's real round, scoped to its
-- competition and season, instead of being derived from the ISO week of the
-- kickoff date (which produced unrelated labels such as "Gameweek 37").
--
-- The columns are intentionally nullable: rows synchronised before this
-- migration have no provider identity, and the matchday must never be guessed
-- or fabricated. Such legacy rows are simply not part of any comparison
-- gameweek until they are re-synchronised.
ALTER TABLE matches
    ADD COLUMN competition_code VARCHAR(20),
    ADD COLUMN season           INTEGER,
    ADD COLUMN matchday         INTEGER;

CREATE INDEX idx_matches_gameweek_identity
    ON matches (competition_code, season, matchday);
