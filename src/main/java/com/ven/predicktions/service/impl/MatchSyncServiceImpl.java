package com.ven.predicktions.service.impl;

import com.ven.predicktions.integration.sports.SportsMatch;
import com.ven.predicktions.integration.sports.SportsProvider;
import com.ven.predicktions.model.Match;
import com.ven.predicktions.repository.MatchRepository;
import com.ven.predicktions.service.MatchSyncService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MatchSyncServiceImpl implements MatchSyncService {

    private final SportsProvider sportsProvider;
    private final MatchRepository matchRepository;

    public MatchSyncServiceImpl(
            SportsProvider sportsProvider,
            MatchRepository matchRepository
    ) {
        this.sportsProvider = sportsProvider;
        this.matchRepository = matchRepository;
    }

    @Override
    @Transactional
    public void synchronizeMatches(String competitionCode, Integer matchday) {

        List<SportsMatch> sportsMatches =
                sportsProvider.getMatches(competitionCode, matchday);

        for (SportsMatch sportsMatch : sportsMatches) {

            Match match = matchRepository
                    .findByExternalId(sportsMatch.externalId())
                    .orElseGet(() -> new Match(
                            sportsMatch.externalId(),
                            sportsMatch.homeTeam(),
                            sportsMatch.awayTeam(),
                            sportsMatch.kickoffAt(),
                            sportsMatch.status()
                    ));

            match.updateFrom(
                    sportsMatch.homeTeam(),
                    sportsMatch.awayTeam(),
                    sportsMatch.kickoffAt(),
                    sportsMatch.homeScore(),
                    sportsMatch.awayScore(),
                    sportsMatch.status()
            );

            matchRepository.save(match);
        }
    }
}