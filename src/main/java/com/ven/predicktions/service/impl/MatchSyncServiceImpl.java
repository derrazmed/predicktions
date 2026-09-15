package com.ven.predicktions.service.impl;

import com.ven.predicktions.integration.sports.SportsMatch;
import com.ven.predicktions.dto.match.MatchSyncRequest;
import com.ven.predicktions.dto.match.MatchSyncResponse;
import com.ven.predicktions.integration.sports.SportsProvider;
import com.ven.predicktions.model.Match;
import com.ven.predicktions.repository.MatchRepository;
import com.ven.predicktions.service.MatchSyncService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;

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
    public void synchronizeFixtures(String competitionCode) {

        List<SportsMatch> sportsMatches =
                sportsProvider.getCompetitionMatches(competitionCode);

        synchronizeMatches(sportsMatches);
    }

    @Override
    @Transactional
    public void synchronizeResults(
            String competitionCode,
            Integer matchday
    ) {

        List<SportsMatch> sportsMatches =
                sportsProvider.getMatchdayMatches(
                        competitionCode,
                        matchday
                );

        synchronizeMatches(sportsMatches);
    }

    @Override
    @Transactional
    public MatchSyncResponse synchronizeManually(MatchSyncRequest request) {
        String competitionCode = request.competition();

        List<SportsMatch> sportsMatches = sportsProvider.getMatches(
                competitionCode,
                request.gameweek(),
                request.date() != null ? request.date() : request.from(),
                request.date() != null ? request.date() : request.to()
        );
        int created = 0;
        int updated = 0;
        int resultsUpdated = 0;

        for (SportsMatch sportsMatch : sportsMatches) {
            Match match = matchRepository.findByExternalId(sportsMatch.externalId())
                    .orElse(null);
            if (match == null) {
                match = new Match(
                        sportsMatch.externalId(),
                        sportsMatch.homeTeam(),
                        sportsMatch.awayTeam(),
                        sportsMatch.kickoffAt(),
                        sportsMatch.status()
                );
                created++;
            } else {
                updated++;
                if (!java.util.Objects.equals(match.getHomeScore(), sportsMatch.homeScore())
                        || !java.util.Objects.equals(match.getAwayScore(), sportsMatch.awayScore())
                        || match.getStatus() != sportsMatch.status()) {
                    resultsUpdated++;
                }
            }
            match.updateFrom(
                    sportsMatch.homeTeam(), sportsMatch.awayTeam(),
                    sportsMatch.kickoffAt(), sportsMatch.homeScore(),
                    sportsMatch.awayScore(), sportsMatch.status()
            );
            matchRepository.save(match);
        }
        return new MatchSyncResponse(
                sportsMatches.size(), created, updated, resultsUpdated, 0, Instant.now()
        );
    }

    private void synchronizeMatches(
            List<SportsMatch> sportsMatches
    ) {
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