package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.user.AdminUserPageResponse;
import com.ven.predicktions.dto.user.AdminUserDetailsResponse;
import com.ven.predicktions.dto.user.AdminLeagueSummary;
import com.ven.predicktions.dto.user.AdminUserResponse;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.exception.LastAdministratorException;
import com.ven.predicktions.exception.LastActiveAdministratorException;
import com.ven.predicktions.model.Role;
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
import org.springframework.transaction.annotation.Transactional;

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
                predictionRepository.sumTotalPointsByUserId(userId),
                leagues,
                leaderboardPosition
        );
    }

    @Override
    @Transactional
    public AdminUserResponse changeUserRole(UUID userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == Role.ADMIN && role == Role.USER) {
            if (userRepository.findAllByRoleForUpdate(Role.ADMIN).size() <= 1) {
                throw new LastAdministratorException();
            }
        }

        if (user.getRole() != role) {
            user.changeRole(role);
        }

        return toResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponse setUserEnabled(UUID userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isEnabled() == enabled) {
            return toResponse(user);
        }

        if (!enabled && user.getRole() == Role.ADMIN
                && userRepository.findEnabledByRoleForUpdate(Role.ADMIN).size() <= 1) {
            throw new LastActiveAdministratorException();
        }

        user.setEnabled(enabled);
        return toResponse(user);
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
