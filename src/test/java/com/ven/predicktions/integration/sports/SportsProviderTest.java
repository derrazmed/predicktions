package com.ven.predicktions.integration.sports;

import com.ven.predicktions.model.MatchStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SportsProviderTest {

    @Test
    void providerShouldReturnNormalizedMatches() {
        SportsProvider provider = (competitionCode, matchday) -> List.of(
                new SportsMatch(
                        "575335",
                        "Fenerbahçe SK",
                        "AS Roma",
                        Instant.parse("2026-09-10T16:45:00Z"),
                        null,
                        null,
                        MatchStatus.SCHEDULED
                )
        );

        List<SportsMatch> matches = provider.getMatches("CL", 1);

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().externalId()).isEqualTo("575335");
        assertThat(matches.getFirst().homeTeam()).isEqualTo("Fenerbahçe SK");
        assertThat(matches.getFirst().awayTeam()).isEqualTo("AS Roma");
        assertThat(matches.getFirst().status()).isEqualTo(MatchStatus.SCHEDULED);
    }
}