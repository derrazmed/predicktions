package com.ven.predicktions.service;

import com.ven.predicktions.integration.sports.SportsMatch;
import com.ven.predicktions.integration.sports.SportsProvider;
import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.MatchStatus;
import com.ven.predicktions.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
class MatchSyncServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MatchSyncService matchSyncService;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private SportsProvider sportsProvider;

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        SportsProvider sportsProvider() {
            return mock(SportsProvider.class);
        }
    }

    @BeforeEach
    void setUp() {
        matchRepository.deleteAll();
        reset(sportsProvider);
    }

    @Test
    void shouldPersistNewMatch() {
        SportsMatch sportsMatch = new SportsMatch(
                "575335",
                "Fenerbahçe SK",
                "AS Roma",
                Instant.parse("2026-09-10T16:45:00Z"),
                null,
                null,
                MatchStatus.SCHEDULED
        );

        when(sportsProvider.getCompetitionMatches("CL"))
                .thenReturn(List.of(sportsMatch));

        matchSyncService.synchronizeFixtures("CL");

        List<Match> matches = matchRepository.findAll();

        assertThat(matches).hasSize(1);

        Match match = matches.getFirst();

        assertThat(match.getExternalId()).isEqualTo("575335");
        assertThat(match.getHomeTeam()).isEqualTo("Fenerbahçe SK");
        assertThat(match.getAwayTeam()).isEqualTo("AS Roma");
        assertThat(match.getKickoffAt())
                .isEqualTo(Instant.parse("2026-09-10T16:45:00Z"));
        assertThat(match.getHomeScore()).isNull();
        assertThat(match.getAwayScore()).isNull();
        assertThat(match.getStatus())
                .isEqualTo(MatchStatus.SCHEDULED);
    }

    @Test
    void shouldUpdateExistingMatchInsteadOfCreatingDuplicate() {
        SportsMatch scheduledMatch = new SportsMatch(
                "575335",
                "Fenerbahçe SK",
                "AS Roma",
                Instant.parse("2026-09-10T16:45:00Z"),
                null,
                null,
                MatchStatus.SCHEDULED
        );

        SportsMatch finishedMatch = new SportsMatch(
                "575335",
                "Fenerbahçe SK",
                "AS Roma",
                Instant.parse("2026-09-10T16:45:00Z"),
                1,
                2,
                MatchStatus.FINISHED
        );

        when(sportsProvider.getCompetitionMatches("CL"))
                .thenReturn(List.of(scheduledMatch));

        when(sportsProvider.getMatchdayMatches("CL", 1))
                .thenReturn(List.of(finishedMatch));

        // Initial fixture synchronization
        matchSyncService.synchronizeFixtures("CL");

        // Result synchronization after the matchday
        matchSyncService.synchronizeResults("CL", 1);

        List<Match> matches = matchRepository.findAll();

        assertThat(matches).hasSize(1);

        Match match = matches.getFirst();

        assertThat(match.getExternalId()).isEqualTo("575335");
        assertThat(match.getHomeScore()).isEqualTo(1);
        assertThat(match.getAwayScore()).isEqualTo(2);
        assertThat(match.getStatus())
                .isEqualTo(MatchStatus.FINISHED);
    }

    @Test
    void shouldPersistMultipleMatchesInBatch() {
        SportsMatch firstMatch = new SportsMatch(
                "575335",
                "Fenerbahçe SK",
                "AS Roma",
                Instant.parse("2026-09-10T16:45:00Z"),
                null,
                null,
                MatchStatus.SCHEDULED
        );

        SportsMatch secondMatch = new SportsMatch(
                "575336",
                "Real Madrid",
                "Manchester City",
                Instant.parse("2026-09-10T19:00:00Z"),
                null,
                null,
                MatchStatus.SCHEDULED
        );

        SportsMatch thirdMatch = new SportsMatch(
                "575337",
                "Barcelona",
                "Bayern München",
                Instant.parse("2026-09-11T19:00:00Z"),
                null,
                null,
                MatchStatus.SCHEDULED
        );

        when(sportsProvider.getCompetitionMatches("CL"))
                .thenReturn(List.of(
                        firstMatch,
                        secondMatch,
                        thirdMatch
                ));

        matchSyncService.synchronizeFixtures("CL");

        List<Match> matches = matchRepository.findAll();

        assertThat(matches).hasSize(3);

        assertThat(matches)
                .extracting(Match::getExternalId)
                .containsExactlyInAnyOrder(
                        "575335",
                        "575336",
                        "575337"
                );
    }

    @Test
    void shouldRollbackSynchronizationWhenPersistenceFails() {
        SportsMatch validMatch = new SportsMatch(
                "575338",
                "Liverpool",
                "Inter Milan",
                Instant.parse("2026-09-12T19:00:00Z"),
                null,
                null,
                MatchStatus.SCHEDULED
        );

        SportsMatch invalidMatch = new SportsMatch(
                "575339",
                null,
                "Barcelona",
                Instant.parse("2026-09-12T21:00:00Z"),
                null,
                null,
                MatchStatus.SCHEDULED
        );

        when(sportsProvider.getCompetitionMatches("CL"))
                .thenReturn(List.of(validMatch, invalidMatch));

        assertThatThrownBy(() ->
                matchSyncService.synchronizeFixtures("CL")
        ).isInstanceOf(Exception.class);

        assertThat(matchRepository.findAll()).isEmpty();
    }
}