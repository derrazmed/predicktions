package com.ven.predicktions.controller;

import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.MatchStatus;
import com.ven.predicktions.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@Transactional
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MatchRepository matchRepository;

    private Match savedMatch;

    @BeforeEach
    void setUp() {
        matchRepository.deleteAll();

        savedMatch = matchRepository.save(new Match(
                "ext-api-1001",
                "Arsenal",
                "Chelsea",
                Instant.parse("2026-09-15T18:00:00Z"),
                MatchStatus.SCHEDULED
        ));
    }

    @Test
    void shouldReturnAllMatches() throws Exception {
        matchRepository.save(new Match(
                "ext-api-1002",
                "Liverpool",
                "Everton",
                Instant.parse("2026-09-16T15:30:00Z"),
                MatchStatus.LIVE
        ));

        mockMvc.perform(get("/api/matches").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].externalId").exists())
                .andExpect(jsonPath("$[0].homeTeam").exists())
                .andExpect(jsonPath("$[0].awayTeam").exists())
                .andExpect(jsonPath("$[0].kickoffAt").exists())
                .andExpect(jsonPath("$[0].status").exists());
    }

    @Test
    void shouldReturnMatchById() throws Exception {
        mockMvc.perform(get("/api/matches/{id}", savedMatch.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedMatch.getId().toString()))
                .andExpect(jsonPath("$.externalId").value("ext-api-1001"))
                .andExpect(jsonPath("$.homeTeam").value("Arsenal"))
                .andExpect(jsonPath("$.awayTeam").value("Chelsea"))
                .andExpect(jsonPath("$.kickoffAt").value("2026-09-15T18:00:00Z"))
                .andExpect(jsonPath("$.homeScore").value(nullValue()))
                .andExpect(jsonPath("$.awayScore").value(nullValue()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void shouldReturnNotFoundForUnknownMatch() throws Exception {
        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(get("/api/matches/{id}", unknownId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Match not found"))
                .andExpect(jsonPath("$.path").value("/api/matches/" + unknownId));
    }
}
