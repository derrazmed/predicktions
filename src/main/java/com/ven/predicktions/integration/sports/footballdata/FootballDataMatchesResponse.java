package com.ven.predicktions.integration.sports.footballdata;

import java.util.List;

public record FootballDataMatchesResponse(
        List<FootballDataMatch> matches
) {}