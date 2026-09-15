package com.ven.predicktions.service.impl;

import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.model.League;
import com.ven.predicktions.repository.LeagueMemberRepository;
import com.ven.predicktions.repository.LeagueRepository;
import com.ven.predicktions.service.AdminLeagueDeletionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminLeagueDeletionServiceImpl implements AdminLeagueDeletionService {

    private static final Logger log =
            LoggerFactory.getLogger(AdminLeagueDeletionServiceImpl.class);

    private final LeagueRepository leagueRepository;
    private final LeagueMemberRepository leagueMemberRepository;

    public AdminLeagueDeletionServiceImpl(
            LeagueRepository leagueRepository,
            LeagueMemberRepository leagueMemberRepository
    ) {
        this.leagueRepository = leagueRepository;
        this.leagueMemberRepository = leagueMemberRepository;
    }

    @Override
    @Transactional
    public void deleteLeague(UUID leagueId, UUID adminId) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new ResourceNotFoundException("League not found"));

        String leagueName = league.getName();
        leagueMemberRepository.deleteAllByLeagueId(leagueId);
        leagueRepository.delete(league);

        log.info("Admin {} deleted league {} ({})", adminId, leagueId, leagueName);
    }
}
