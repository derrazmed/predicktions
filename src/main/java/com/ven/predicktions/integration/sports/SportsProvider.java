package com.ven.predicktions.integration.sports;

import java.util.List;
import java.time.LocalDate;

public interface SportsProvider {
    List<SportsMatch> getCompetitionMatches(String competitionCode);

    List<SportsMatch> getMatchdayMatches(String competitionCode, Integer matchday);

    default List<SportsMatch> getMatches(
            String competitionCode,
            Integer matchday,
            LocalDate from,
            LocalDate to
    ) {
        if (matchday != null) {
            return getMatchdayMatches(competitionCode, matchday);
        }
        if (from == null && to == null) {
            return getCompetitionMatches(competitionCode);
        }
        return getCompetitionMatches(competitionCode, from, to);
    }

    default List<SportsMatch> getCompetitionMatches(
            String competitionCode,
            LocalDate from,
            LocalDate to
    ) {
        return getCompetitionMatches(competitionCode);
    }
}