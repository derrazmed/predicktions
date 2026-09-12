package com.ven.predicktions.service;

import com.ven.predicktions.dto.league.CreateLeagueRequest;
import com.ven.predicktions.dto.league.LeagueResponse;

import java.util.UUID;

public interface LeagueService {

    LeagueResponse createLeague(UUID userId, CreateLeagueRequest request);
}
