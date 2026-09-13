package com.ven.predicktions.mapper;

import com.ven.predicktions.dto.league.LeagueResponse;
import com.ven.predicktions.model.League;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LeagueMapper {

    public LeagueResponse toResponse(
            League league,
            long memberCount,
            List<String> memberUsernames
    ) {
        return new LeagueResponse(
                league.getId(),
                league.getName(),
                league.getOwner().getId(),
                memberCount,
                memberUsernames,
                league.getJoinCode(),
                league.getCreatedAt()
        );
    }
}
