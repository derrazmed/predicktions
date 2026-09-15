package com.ven.predicktions.service;

import com.ven.predicktions.dto.match.MatchRecalculationResponse;
import com.ven.predicktions.model.Match;

import java.util.UUID;

public interface PredictionRecalculationService {

    MatchRecalculationResponse recalculate(Match match, UUID adminId);
}
