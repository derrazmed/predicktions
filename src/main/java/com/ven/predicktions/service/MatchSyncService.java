package com.ven.predicktions.service;

import com.ven.predicktions.dto.match.MatchSyncRequest;
import com.ven.predicktions.dto.match.MatchSyncResponse;

public interface MatchSyncService {

    void synchronizeFixtures(String competitionCode);

    void synchronizeResults(String competitionCode, Integer matchday);

    MatchSyncResponse synchronizeManually(MatchSyncRequest request);
}