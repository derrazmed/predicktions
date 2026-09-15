package com.ven.predicktions.service;

import java.util.UUID;

public interface AdminLeagueMembershipService {

    void removeMember(UUID leagueId, UUID userId, UUID adminId);
}
