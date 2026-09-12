package com.ven.predicktions.service;

import com.ven.predicktions.dto.match.MatchResponse;
import com.ven.predicktions.mapper.MatchMapper;
import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.MatchStatus;
import com.ven.predicktions.repository.MatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchMapper matchMapper;

    @InjectMocks
    private MatchService matchService;

    @Test
    void shouldRetrieveMatchesFromLocalRepository() {
        Match match = new Match(
                "575335",
                "Fenerbahçe SK",
                "AS Roma",
                Instant.parse("2026-09-10T16:45:00Z"),
                MatchStatus.SCHEDULED
        );

        MatchResponse response = new MatchResponse(
                match.getId(),
                match.getExternalId(),
                match.getHomeTeam(),
                match.getAwayTeam(),
                match.getKickoffAt(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.getStatus()
        );

        when(matchRepository.findAll())
                .thenReturn(List.of(match));

        when(matchMapper.toResponse(match))
                .thenReturn(response);

        List<MatchResponse> result = matchService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(response);

        verify(matchRepository).findAll();
        verify(matchMapper).toResponse(match);
    }
}