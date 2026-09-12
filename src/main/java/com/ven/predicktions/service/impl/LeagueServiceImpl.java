package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.league.CreateLeagueRequest;
import com.ven.predicktions.dto.league.LeagueResponse;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.mapper.LeagueMapper;
import com.ven.predicktions.model.League;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.LeagueRepository;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.JoinCodeGenerator;
import com.ven.predicktions.service.LeagueService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class LeagueServiceImpl implements LeagueService {

    static final int MAX_JOIN_CODE_ATTEMPTS = 10;

    private final LeagueRepository leagueRepository;
    private final UserRepository userRepository;
    private final JoinCodeGenerator joinCodeGenerator;
    private final LeagueMapper leagueMapper;

    public LeagueServiceImpl(
            LeagueRepository leagueRepository,
            UserRepository userRepository,
            JoinCodeGenerator joinCodeGenerator,
            LeagueMapper leagueMapper
    ) {
        this.leagueRepository = leagueRepository;
        this.userRepository = userRepository;
        this.joinCodeGenerator = joinCodeGenerator;
        this.leagueMapper = leagueMapper;
    }

    @Override
    public LeagueResponse createLeague(UUID userId, CreateLeagueRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String joinCode = generateUniqueJoinCode();
        League league = new League(request.name(), owner, joinCode);
        League savedLeague = leagueRepository.save(league);

        return leagueMapper.toResponse(savedLeague);
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
