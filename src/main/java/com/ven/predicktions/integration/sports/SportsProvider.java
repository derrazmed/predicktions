package com.ven.predicktions.integration.sports;

import java.util.List;

public interface SportsProvider {
    List<SportsMatch> getCompetitionMatches(String competitionCode);

    List<SportsMatch> getMatchdayMatches(String competitionCode, Integer matchday);
}