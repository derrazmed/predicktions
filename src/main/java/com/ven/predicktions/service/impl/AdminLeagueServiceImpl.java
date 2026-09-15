package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.league.AdminLeagueOwnerResponse;
import com.ven.predicktions.dto.league.AdminLeaguePageResponse;
import com.ven.predicktions.dto.league.AdminLeagueResponse;
import com.ven.predicktions.repository.AdminLeagueProjection;
import com.ven.predicktions.repository.LeagueRepository;
import com.ven.predicktions.service.AdminLeagueService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminLeagueServiceImpl implements AdminLeagueService {

    private static final int MAX_PAGE_SIZE = 100;

    private final LeagueRepository leagueRepository;

    public AdminLeagueServiceImpl(LeagueRepository leagueRepository) {
        this.leagueRepository = leagueRepository;
    }

    @Override
    public AdminLeaguePageResponse getLeagues(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }

        Page<AdminLeagueProjection> leagues = leagueRepository.findAllForAdministration(
                PageRequest.of(page, size)
        );

        List<AdminLeagueResponse> content = leagues.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new AdminLeaguePageResponse(
                content,
                leagues.getNumber(),
                leagues.getSize(),
                leagues.getTotalElements(),
                leagues.getTotalPages()
        );
    }

    private AdminLeagueResponse toResponse(AdminLeagueProjection league) {
        return new AdminLeagueResponse(
                league.getId(),
                league.getName(),
                new AdminLeagueOwnerResponse(
                        league.getOwnerId(),
                        league.getOwnerUsername()
                ),
                league.getMemberCount(),
                league.getCreatedAt(),
                league.getJoinCode()
        );
    }
}
