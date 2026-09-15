package com.ven.predicktions.service;

import com.ven.predicktions.dto.match.MatchResponse;
import com.ven.predicktions.dto.match.MatchRecalculationResponse;
import com.ven.predicktions.dto.match.UpdateMatchResultRequest;

import java.util.UUID;

public interface AdminMatchService {

    MatchResponse updateResult(UUID matchId, UpdateMatchResultRequest request, UUID adminId);

    MatchRecalculationResponse recalculate(UUID matchId, UUID adminId);
}
