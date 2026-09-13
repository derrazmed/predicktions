package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.leaderboard.LeaderboardEntryResponse;
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

@Service
@Transactional(readOnly = true)
public class LeaderboardServiceImpl implements LeaderboardService {

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

        List<Object[]> results =
                predictionRepository.findGlobalLeaderboard();

        long previousPoints = Long.MIN_VALUE;
        int currentRank = 0;

        List<LeaderboardEntryResponse> leaderboard = new java.util.ArrayList<>();

        for (int i = 0; i < results.size(); i++) {

            Object[] row = results.get(i);

            UUID userId = (UUID) row[0];
            String username = (String) row[1];
            long totalPoints = ((Number) row[2]).longValue();

            if (totalPoints != previousPoints) {
                currentRank = i + 1;
                previousPoints = totalPoints;
            }

            leaderboard.add(
                    new LeaderboardEntryResponse(
                            currentRank,
                            userId,
                            username,
                            totalPoints
                    )
            );
        }

        return leaderboard;
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

        List<Object[]> results =
                predictionRepository.findLeagueLeaderboard(leagueId);

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