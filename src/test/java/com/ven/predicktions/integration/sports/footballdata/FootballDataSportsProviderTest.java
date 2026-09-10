package com.ven.predicktions.integration.sports.footballdata;

import com.ven.predicktions.integration.sports.SportsMatch;
import com.ven.predicktions.model.MatchStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class FootballDataSportsProviderTest {

    private static final String API_TOKEN = "test-api-token";

    private MockRestServiceServer mockServer;
    private FootballDataSportsProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl("https://api.football-data.org/v4")
                .defaultHeader("X-Auth-Token", API_TOKEN);

        mockServer = MockRestServiceServer
                .bindTo(restClientBuilder)
                .build();

        RestClient restClient = restClientBuilder.build();

        provider = new FootballDataSportsProvider(restClient);
    }

    @Test
    void shouldRetrieveAndNormalizeMatches() {

        String responseBody = """
                {
                  "matches": [
                    {
                      "id": 575335,
                      "utcDate": "2026-09-10T16:45:00Z",
                      "status": "TIMED",
                      "homeTeam": {
                        "id": 102,
                        "name": "Fenerbahçe SK",
                        "shortName": "Fenerbahçe",
                        "tla": "FEN"
                      },
                      "awayTeam": {
                        "id": 100,
                        "name": "AS Roma",
                        "shortName": "Roma",
                        "tla": "ROM"
                      },
                      "score": {
                        "fullTime": {
                          "home": null,
                          "away": null
                        }
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(
                        "https://api.football-data.org/v4/competitions/CL/matches?matchday=1"
                ))
                .andExpect(method(GET))
                .andExpect(header("X-Auth-Token", API_TOKEN))
                .andRespond(withSuccess(
                        responseBody,
                        MediaType.APPLICATION_JSON
                ));

        List<SportsMatch> matches =
                provider.getMatches("CL", 1);

        assertThat(matches).hasSize(1);

        SportsMatch match = matches.getFirst();

        assertThat(match.externalId()).isEqualTo("575335");
        assertThat(match.homeTeam()).isEqualTo("Fenerbahçe SK");
        assertThat(match.awayTeam()).isEqualTo("AS Roma");
        assertThat(match.kickoffAt())
                .isEqualTo(Instant.parse("2026-09-10T16:45:00Z"));
        assertThat(match.homeScore()).isNull();
        assertThat(match.awayScore()).isNull();
        assertThat(match.status()).isEqualTo(MatchStatus.SCHEDULED);

        mockServer.verify();
    }

    @Test
    void shouldMapFinishedMatchAndScores() {

        String responseBody = """
                {
                  "matches": [
                    {
                      "id": 575323,
                      "utcDate": "2026-09-08T16:45:00Z",
                      "status": "FINISHED",
                      "homeTeam": {
                        "id": 1,
                        "name": "Club Brugge KV"
                      },
                      "awayTeam": {
                        "id": 2,
                        "name": "Aston Villa FC"
                      },
                      "score": {
                        "fullTime": {
                          "home": 2,
                          "away": 3
                        }
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(
                        "https://api.football-data.org/v4/competitions/CL/matches?matchday=1"
                ))
                .andExpect(method(GET))
                .andExpect(header("X-Auth-Token", API_TOKEN))
                .andRespond(withSuccess(
                        responseBody,
                        MediaType.APPLICATION_JSON
                ));

        List<SportsMatch> matches =
                provider.getMatches("CL", 1);

        assertThat(matches).hasSize(1);

        SportsMatch match = matches.getFirst();

        assertThat(match.externalId()).isEqualTo("575323");
        assertThat(match.homeScore()).isEqualTo(2);
        assertThat(match.awayScore()).isEqualTo(3);
        assertThat(match.status()).isEqualTo(MatchStatus.FINISHED);

        mockServer.verify();
    }

    @Test
    void shouldMapLiveStatuses() {

        String responseBody = """
                {
                  "matches": [
                    {
                      "id": 123456,
                      "utcDate": "2026-09-10T16:45:00Z",
                      "status": "IN_PLAY",
                      "homeTeam": {
                        "id": 1,
                        "name": "Team A"
                      },
                      "awayTeam": {
                        "id": 2,
                        "name": "Team B"
                      },
                      "score": {
                        "fullTime": {
                          "home": 1,
                          "away": 0
                        }
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(
                        "https://api.football-data.org/v4/competitions/CL/matches?matchday=1"
                ))
                .andRespond(withSuccess(
                        responseBody,
                        MediaType.APPLICATION_JSON
                ));

        List<SportsMatch> matches =
                provider.getMatches("CL", 1);

        assertThat(matches.getFirst().status())
                .isEqualTo(MatchStatus.LIVE);

        mockServer.verify();
    }

    @Test
    void shouldReturnEmptyListWhenResponseContainsNoMatches() {

        String responseBody = """
                {
                  "matches": []
                }
                """;

        mockServer.expect(requestTo(
                        "https://api.football-data.org/v4/competitions/CL/matches?matchday=1"
                ))
                .andRespond(withSuccess(
                        responseBody,
                        MediaType.APPLICATION_JSON
                ));

        List<SportsMatch> matches =
                provider.getMatches("CL", 1);

        assertThat(matches).isEmpty();

        mockServer.verify();
    }
}