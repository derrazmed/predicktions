package com.ven.predicktions.service;

import java.util.UUID;

public interface AdminLeagueDeletionService {

    void deleteLeague(UUID leagueId, UUID adminId);
}
