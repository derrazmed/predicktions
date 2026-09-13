package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.league.CreateLeagueRequest;
import com.ven.predicktions.dto.league.JoinLeagueRequest;
import com.ven.predicktions.dto.league.LeagueResponse;
import com.ven.predicktions.exception.DuplicateResourceException;
import com.ven.predicktions.exception.OwnerCannotLeaveException;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.mapper.LeagueMapper;
import com.ven.predicktions.model.League;
import com.ven.predicktions.model.LeagueMember;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.LeagueMemberRepository;
import com.ven.predicktions.repository.LeagueRepository;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.JoinCodeGenerator;
import com.ven.predicktions.service.LeagueService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class LeagueServiceImpl implements LeagueService {

    static final int MAX_JOIN_CODE_ATTEMPTS = 10;

    private final LeagueRepository leagueRepository;
    private final LeagueMemberRepository leagueMemberRepository;
    private final UserRepository userRepository;
    private final JoinCodeGenerator joinCodeGenerator;
    private final LeagueMapper leagueMapper;

    public LeagueServiceImpl(
            LeagueRepository leagueRepository,
            LeagueMemberRepository leagueMemberRepository,
            UserRepository userRepository,
            JoinCodeGenerator joinCodeGenerator,
            LeagueMapper leagueMapper
    ) {
        this.leagueRepository = leagueRepository;
        this.leagueMemberRepository = leagueMemberRepository;
        this.userRepository = userRepository;
        this.joinCodeGenerator = joinCodeGenerator;
        this.leagueMapper = leagueMapper;
    }

    @Override
    public LeagueResponse createLeague(UUID userId, CreateLeagueRequest request) {
        User owner = findUser(userId);

        String joinCode = generateUniqueJoinCode();
        League league = new League(request.name(), owner, joinCode);
        League savedLeague = leagueRepository.save(league);

        return leagueMapper.toResponse(savedLeague);
    }

    @Override
    public LeagueResponse joinLeague(UUID userId, JoinLeagueRequest request) {
        User user = findUser(userId);
        String joinCode = normalizeJoinCode(request.joinCode());

        League league = leagueRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid join code"));

        if (leagueMemberRepository.existsByLeagueIdAndUserId(league.getId(), userId)) {
            throw new DuplicateResourceException("User is already a member of this league");
        }

        leagueMemberRepository.save(new LeagueMember(league, user));

        return leagueMapper.toResponse(league);
    }

    @Override
    public void leaveLeague(UUID userId, UUID leagueId) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new ResourceNotFoundException("League not found"));

        if (league.getOwner().getId().equals(userId)) {
            throw new OwnerCannotLeaveException(
                    "League owner cannot leave the league"
            );
        }

        LeagueMember membership = leagueMemberRepository
                .findByLeagueIdAndUserId(leagueId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

        leagueMemberRepository.delete(membership);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String normalizeJoinCode(String joinCode) {
        return joinCode.trim().toUpperCase(Locale.ROOT);
    }

    private String generateUniqueJoinCode() {
        for (int attempt = 0; attempt < MAX_JOIN_CODE_ATTEMPTS; attempt++) {
            String joinCode = joinCodeGenerator.generate();

            if (!leagueRepository.existsByJoinCode(joinCode)) {
                return joinCode;
            }
        }

        throw new IllegalStateException("Unable to generate a unique join code");
    }
}
