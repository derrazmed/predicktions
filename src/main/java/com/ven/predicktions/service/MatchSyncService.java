package com.ven.predicktions.service;

public interface MatchSyncService {

    void synchronizeFixtures(String competitionCode);

    void synchronizeResults(String competitionCode, Integer matchday);
}