package com.ven.predicktions.repository;

import java.time.Instant;
import java.util.UUID;

public interface AdminLeagueProjection {

    UUID getId();

    String getName();

    UUID getOwnerId();

    String getOwnerUsername();

    long getMemberCount();

    Instant getCreatedAt();

    String getJoinCode();
}
