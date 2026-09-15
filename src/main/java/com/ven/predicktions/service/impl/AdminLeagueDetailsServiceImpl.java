package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.leaderboard.LeaderboardEntryResponse;
import com.ven.predicktions.dto.league.*;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.model.League;
import com.ven.predicktions.repository.LeagueMemberRepository;
import com.ven.predicktions.repository.LeaguePredictionStatistics;
import com.ven.predicktions.repository.LeagueRepository;
import com.ven.predicktions.repository.PredictionRepository;
import com.ven.predicktions.service.AdminLeagueDetailsService;
import com.ven.predicktions.service.LeaderboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminLeagueDetailsServiceImpl implements AdminLeagueDetailsService {

    private final LeagueRepository leagueRepository;
    private final LeagueMemberRepository leagueMemberRepository;
    private final LeaderboardService leaderboardService;
    private final PredictionRepository predictionRepository;

    public AdminLeagueDetailsServiceImpl(
            LeagueRepository leagueRepository,
            LeagueMemberRepository leagueMemberRepository,
            LeaderboardService leaderboardService,
            PredictionRepository predictionRepository
    ) {
        this.leagueRepository = leagueRepository;
        this.leagueMemberRepository = leagueMemberRepository;
        this.leaderboardService = leaderboardService;
        this.predictionRepository = predictionRepository;
    }

    @Override
    public AdminLeagueDetailsResponse getLeague(UUID leagueId) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new ResourceNotFoundException("League not found"));

        List<AdminLeagueMemberResponse> members = leagueMemberRepository
                .findAllByLeagueIdOrdered(leagueId)
                .stream()
                .map(member -> new AdminLeagueMemberResponse(
                        member.getUser().getId(),
                        member.getUser().getUsername(),
                        member.getCreatedAt(),
                        member.getUser().getRole()
                ))
                .toList();

        List<LeaderboardEntryResponse> leaderboard =
                leaderboardService.getLeagueLeaderboardForAdmin(leagueId);
        LeaguePredictionStatistics stats =
                predictionRepository.findLeaguePredictionStatistics(leagueId);

        long totalPredictions = stats.getTotalPredictions();
        long totalPoints = stats.getTotalPointsAwarded();

        return new AdminLeagueDetailsResponse(
                league.getId(),
                league.getName(),
                league.getCreatedAt(),
                league.getJoinCode(),
                members.size(),
                new AdminLeagueOwnerResponse(
                        league.getOwner().getId(),
                        league.getOwner().getUsername()
                ),
                members,
                leaderboard,
                new AdminLeagueStatisticsResponse(
                        totalPredictions,
                        stats.getPredictionsWithPoints(),
                        totalPoints,
                        totalPredictions == 0 ? 0.0 : (double) totalPoints / totalPredictions,
                        stats.getExactScorePredictions(),
                        stats.getParticipatingMembers()
                )
        );
    }
}
