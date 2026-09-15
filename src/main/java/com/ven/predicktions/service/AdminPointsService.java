package com.ven.predicktions.service;

import com.ven.predicktions.dto.user.AddPointsRequest;
import com.ven.predicktions.dto.user.AddPointsResponse;

import java.util.UUID;

public interface AdminPointsService {

    AddPointsResponse addPoints(UUID userId, AddPointsRequest request, UUID adminUserId);
}
