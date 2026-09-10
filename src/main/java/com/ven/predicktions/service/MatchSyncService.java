package com.ven.predicktions.service;

public interface MatchSyncService {

    void synchronizeMatches(String competitionCode, Integer matchday);
}