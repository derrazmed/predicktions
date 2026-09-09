package com.ven.predicktions.service;

import com.ven.predicktions.dto.match.MatchResponse;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.mapper.MatchMapper;
import com.ven.predicktions.repository.MatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchMapper matchMapper;

    public MatchService(MatchRepository matchRepository, MatchMapper matchMapper) {
        this.matchRepository = matchRepository;
        this.matchMapper = matchMapper;
    }

    public List<MatchResponse> findAll() {
        return matchRepository.findAll().stream()
                .map(matchMapper::toResponse)
                .toList();
    }

    public MatchResponse findById(UUID id) {
        return matchRepository.findById(id)
                .map(matchMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
    }
}
