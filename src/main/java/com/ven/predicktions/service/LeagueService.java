package com.ven.predicktions.service;

import com.ven.predicktions.dto.league.CreateLeagueRequest;
import com.ven.predicktions.dto.league.JoinLeagueRequest;
import com.ven.predicktions.dto.league.LeagueResponse;
import com.ven.predicktions.dto.league.UpdateLeagueRequest;

import java.util.List;
import java.util.UUID;

public interface LeagueService {

    LeagueResponse createLeague(UUID userId, CreateLeagueRequest request);

    LeagueResponse joinLeague(UUID userId, JoinLeagueRequest request);

    void leaveLeague(UUID userId, UUID leagueId);

    List<LeagueResponse> getLeaguesForUser(UUID userId);

    LeagueResponse getLeague(UUID userId, UUID leagueId);

    LeagueResponse updateLeague(UUID userId, UUID leagueId, UpdateLeagueRequest request);

    void removeMember(UUID userId, UUID leagueId, UUID memberId);
}
