package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.leaderboard.LeaderboardEntryResponse;
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

    public LeaderboardServiceImpl(PredictionRepository predictionRepository) {
        this.predictionRepository = predictionRepository;
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
}