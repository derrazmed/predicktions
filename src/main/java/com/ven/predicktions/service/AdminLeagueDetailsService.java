package com.ven.predicktions.service;

import com.ven.predicktions.dto.league.AdminLeagueDetailsResponse;

import java.util.UUID;

public interface AdminLeagueDetailsService {

    AdminLeagueDetailsResponse getLeague(UUID leagueId);
}
