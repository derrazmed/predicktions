package com.ven.predicktions.integration.sports.footballdata;

import com.ven.predicktions.integration.sports.SportsMatch;
import com.ven.predicktions.integration.sports.SportsProvider;
import com.ven.predicktions.model.MatchStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class FootballDataSportsProvider implements SportsProvider {

    private final RestClient restClient;

    public FootballDataSportsProvider(
            RestClient restClient
    ) {
        this.restClient = restClient;
    }

    @Override
    public List<SportsMatch> getMatches(String competitionCode, Integer matchday) {

        FootballDataMatchesResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/competitions/{competitionCode}/matches")
                        .queryParam("matchday", matchday)
                        .build(competitionCode))
                .retrieve()
                .body(FootballDataMatchesResponse.class);

        if (response == null || response.matches() == null) {
            return List.of();
        }

        return response.matches()
                .stream()
                .map(this::toSportsMatch)
                .toList();
    }

    private SportsMatch toSportsMatch(FootballDataMatch match) {

        Integer homeScore = match.score() != null
                ? match.score().fullTime().home()
                : null;

        Integer awayScore = match.score() != null
                ? match.score().fullTime().away()
                : null;

        return new SportsMatch(
                String.valueOf(match.id()),
                match.homeTeam().name(),
                match.awayTeam().name(),
                match.utcDate(),
                homeScore,
                awayScore,
                mapStatus(match.status())
        );
    }

    private MatchStatus mapStatus(String status) {

        return switch (status) {
            case "TIMED" -> MatchStatus.SCHEDULED;
            case "IN_PLAY", "PAUSED" -> MatchStatus.LIVE;
            case "FINISHED" -> MatchStatus.FINISHED;
            case "CANCELLED" -> MatchStatus.CANCELLED;
            case "POSTPONED", "SUSPENDED" -> MatchStatus.SCHEDULED;
            default -> throw new IllegalArgumentException(
                    "Unsupported football-data.org match status: " + status
            );
        };
    }
}