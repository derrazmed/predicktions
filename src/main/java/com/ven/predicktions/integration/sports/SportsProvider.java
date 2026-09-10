package com.ven.predicktions.integration.sports;

import java.util.List;

public interface SportsProvider {

    List<SportsMatch> getMatches(String competitionCode, Integer matchday);
}