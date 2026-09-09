package com.ven.predicktions.repository;

import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.MatchStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class MatchRepositoryTest {

    @Autowired
    private MatchRepository matchRepository;

    @Test
    void shouldPersistAndFindMatchByExternalId() {
        Instant kickoffAt = Instant.parse("2026-09-15T18:00:00Z");
        Match match = new Match(
                "ext-match-1001",
                "Arsenal",
                "Chelsea",
                kickoffAt,
                MatchStatus.SCHEDULED
        );

        matchRepository.save(match);

        Optional<Match> result = matchRepository.findByExternalId("ext-match-1001");

        assertThat(result).isPresent();
        assertThat(result.get().getExternalId()).isEqualTo("ext-match-1001");
        assertThat(result.get().getHomeTeam()).isEqualTo("Arsenal");
        assertThat(result.get().getAwayTeam()).isEqualTo("Chelsea");
        assertThat(result.get().getKickoffAt()).isEqualTo(kickoffAt);
        assertThat(result.get().getHomeScore()).isNull();
        assertThat(result.get().getAwayScore()).isNull();
        assertThat(result.get().getStatus()).isEqualTo(MatchStatus.SCHEDULED);
    }

    @Test
    void shouldUpdateScoresAndStatus() {
        Match match = new Match(
                "ext-match-1002",
                "Liverpool",
                "Everton",
                Instant.parse("2026-09-16T15:30:00Z"),
                MatchStatus.SCHEDULED
        );

        Match saved = matchRepository.save(match);

        saved.setHomeScore(2);
        saved.setAwayScore(1);
        saved.setStatus(MatchStatus.FINISHED);
        matchRepository.save(saved);

        Optional<Match> result = matchRepository.findByExternalId("ext-match-1002");

        assertThat(result).isPresent();
        assertThat(result.get().getHomeScore()).isEqualTo(2);
        assertThat(result.get().getAwayScore()).isEqualTo(1);
        assertThat(result.get().getStatus()).isEqualTo(MatchStatus.FINISHED);
    }

    @Test
    void shouldRejectDuplicateExternalId() {
        Match first = new Match(
                "ext-match-dup",
                "Arsenal",
                "Chelsea",
                Instant.parse("2026-09-15T18:00:00Z"),
                MatchStatus.SCHEDULED
        );
        matchRepository.saveAndFlush(first);

        Match duplicate = new Match(
                "ext-match-dup",
                "Liverpool",
                "Everton",
                Instant.parse("2026-09-16T15:30:00Z"),
                MatchStatus.SCHEDULED
        );

        assertThatThrownBy(() -> matchRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldCheckExternalIdExistence() {
        Match match = new Match(
                "ext-match-1003",
                "Tottenham",
                "West Ham",
                Instant.parse("2026-09-17T19:45:00Z"),
                MatchStatus.SCHEDULED
        );
        matchRepository.save(match);

        assertThat(matchRepository.existsByExternalId("ext-match-1003")).isTrue();
        assertThat(matchRepository.existsByExternalId("unknown")).isFalse();
    }
}
