package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.dashboard.AdminDashboardResponse;
import com.ven.predicktions.model.Role;
import com.ven.predicktions.repository.LeagueRepository;
import com.ven.predicktions.repository.MatchRepository;
import com.ven.predicktions.repository.PredictionRepository;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.AdminDashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final LeagueRepository leagueRepository;
    private final PredictionRepository predictionRepository;
    private final MatchRepository matchRepository;

    public AdminDashboardServiceImpl(
            UserRepository userRepository,
            LeagueRepository leagueRepository,
            PredictionRepository predictionRepository,
            MatchRepository matchRepository
    ) {
        this.userRepository = userRepository;
        this.leagueRepository = leagueRepository;
        this.predictionRepository = predictionRepository;
        this.matchRepository = matchRepository;
    }

    @Override
    public AdminDashboardResponse getDashboard() {
        Instant startOfToday = LocalDate.now(ZoneOffset.UTC)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
        Instant startOfTomorrow = startOfToday.plus(java.time.Duration.ofDays(1));

        return new AdminDashboardResponse(
                userRepository.count(),
                userRepository.countByEnabledTrue(),
                userRepository.countByRole(Role.ADMIN),
                leagueRepository.count(),
                predictionRepository.count(),
                matchRepository.count(),
                predictionRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        startOfToday,
                        startOfTomorrow
                )
        );
    }
}
