package com.ven.predicktions.mapper;

import com.ven.predicktions.dto.MatchResponse;
import com.ven.predicktions.model.Match;
import org.springframework.stereotype.Component;

@Component
public class MatchMapper {

    public MatchResponse toResponse(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getExternalId(),
                match.getHomeTeam(),
                match.getAwayTeam(),
                match.getKickoffAt(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.getStatus()
        );
    }
}
