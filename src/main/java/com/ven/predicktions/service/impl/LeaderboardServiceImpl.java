package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.leaderboard.LeaderboardEntryResponse;
import com.ven.predicktions.dto.leaderboard.LeaderboardRecalculationResponse;
import com.ven.predicktions.exception.ForbiddenOperationException;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.repository.LeagueMemberRepository;
import com.ven.predicktions.repository.LeagueRepository;
import com.ven.predicktions.repository.PredictionRepository;
import com.ven.predicktions.service.LeaderboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional(readOnly = true)
public class LeaderboardServiceImpl implements LeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardServiceImpl.class);
    private final PredictionRepository predictionRepository;
    private final LeagueRepository leagueRepository;
    private final LeagueMemberRepository leagueMemberRepository;

    public LeaderboardServiceImpl(PredictionRepository predictionRepository, LeagueRepository leagueRepository, LeagueMemberRepository leagueMemberRepository) {
        this.predictionRepository = predictionRepository;
        this.leagueRepository = leagueRepository;
        this.leagueMemberRepository = leagueMemberRepository;
    }

    @Override
    public List<LeaderboardEntryResponse> getGlobalLeaderboard() {
        return mapLeaderboard(predictionRepository.findGlobalLeaderboard());
    }

    @Override
    public List<LeaderboardEntryResponse> getGlobalLeaderboard(
            Integer gameweek,
            Integer season
    ) {
        validateFilters(gameweek, season);

        if (gameweek == null && season == null) {
            return getGlobalLeaderboard();
        }

        return mapLeaderboard(
                predictionRepository.findGlobalLeaderboard(season, gameweek)
        );
    }

    @Override
    public LeaderboardRecalculationResponse recalculateGlobalLeaderboard(UUID adminId) {
        List<LeaderboardEntryResponse> leaderboard = getGlobalLeaderboard();
        int usersProcessed = leaderboard.size();

        log.info(
                "Admin {} recalculated global leaderboard: {} users processed, {} entries updated",
                adminId,
                usersProcessed,
                0
        );

        return new LeaderboardRecalculationResponse(
                Instant.now(),
                usersProcessed,
                0,
                usersProcessed,
                "Leaderboard recalculated from prediction data."
        );
    }

    private void validateFilters(Integer gameweek, Integer season) {
        if (gameweek != null && (gameweek < 1 || gameweek > 53)) {
            throw new IllegalArgumentException("gameweek must be between 1 and 53");
        }
        if (season != null && (season < 1 || season > 9999)) {
            throw new IllegalArgumentException("season must be between 1 and 9999");
        }
    }

    @Override
    public List<LeaderboardEntryResponse> getLeagueLeaderboard(
            UUID userId,
            UUID leagueId
    ) {
        leagueRepository.findById(leagueId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("League not found")
                );

        if (!leagueMemberRepository.existsByLeagueIdAndUserId(
                leagueId,
                userId
        )) {
            throw new ForbiddenOperationException(
                    "User is not a member of this league"
            );
        }

        return mapLeaderboard(predictionRepository.findLeagueLeaderboard(leagueId));
    }

    @Override
    public List<LeaderboardEntryResponse> getLeagueLeaderboardForAdmin(UUID leagueId) {
        leagueRepository.findById(leagueId)
                .orElseThrow(() -> new ResourceNotFoundException("League not found"));

        return mapLeaderboard(predictionRepository.findLeagueLeaderboard(leagueId));
    }

    private List<LeaderboardEntryResponse> mapLeaderboard(List<Object[]> results) {

        long previousPoints = Long.MIN_VALUE;
        int currentRank = 0;

        List<LeaderboardEntryResponse> leaderboard =
                new java.util.ArrayList<>();

        for (int i = 0; i < results.size(); i++) {

            Object[] row = results.get(i);

            UUID memberId = (UUID) row[0];
            String username = (String) row[1];
            long totalPoints = ((Number) row[2]).longValue();

            if (totalPoints != previousPoints) {
                currentRank = i + 1;
                previousPoints = totalPoints;
            }

            leaderboard.add(
                    new LeaderboardEntryResponse(
                            currentRank,
                            memberId,
                            username,
                            totalPoints
                    )
            );
        }

        return leaderboard;
    }
}