package com.ven.predicktions.service;

import com.ven.predicktions.dto.league.AdminLeaguePageResponse;

public interface AdminLeagueService {

    AdminLeaguePageResponse getLeagues(int page, int size);
}
