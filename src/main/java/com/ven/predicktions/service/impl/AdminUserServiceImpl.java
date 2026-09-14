package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.user.AdminUserPageResponse;
import com.ven.predicktions.dto.user.AdminUserDetailsResponse;
import com.ven.predicktions.dto.user.AdminLeagueSummary;
import com.ven.predicktions.dto.user.AdminUserResponse;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.LeagueMemberRepository;
import com.ven.predicktions.repository.PredictionRepository;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.AdminUserService;
import com.ven.predicktions.service.LeaderboardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final PredictionRepository predictionRepository;
    private final LeagueMemberRepository leagueMemberRepository;
    private final LeaderboardService leaderboardService;

    public AdminUserServiceImpl(
            UserRepository userRepository,
            PredictionRepository predictionRepository,
            LeagueMemberRepository leagueMemberRepository,
            LeaderboardService leaderboardService
    ) {
        this.userRepository = userRepository;
        this.predictionRepository = predictionRepository;
        this.leagueMemberRepository = leagueMemberRepository;
        this.leaderboardService = leaderboardService;
    }

    @Override
    public AdminUserDetailsResponse getUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Integer leaderboardPosition = leaderboardService.getGlobalLeaderboard()
                .stream()
                .filter(entry -> entry.userId().equals(userId))
                .map(entry -> entry.rank())
                .findFirst()
                .orElse(null);

        List<AdminLeagueSummary> leagues = leagueMemberRepository
                .findAllByUserId(userId)
                .stream()
                .map(member -> new AdminLeagueSummary(
                        member.getLeague().getId(),
                        member.getLeague().getName()
                ))
                .toList();

        return new AdminUserDetailsResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.isEnabled(),
                predictionRepository.countByUserId(userId),
                predictionRepository.sumPointsByUserId(userId),
                leagues,
                leaderboardPosition
        );
    }

    @Override
    public AdminUserPageResponse getUsers(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }

        Page<User> users = userRepository.findAll(
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
        );

        return new AdminUserPageResponse(
                users.map(this::toResponse).getContent(),
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages()
        );
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.isEnabled()
        );
    }
}
