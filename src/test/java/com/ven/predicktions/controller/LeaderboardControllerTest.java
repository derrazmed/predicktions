package com.ven.predicktions.controller;

import com.ven.predicktions.dto.leaderboard.LeaderboardEntryResponse;
import com.ven.predicktions.service.LeaderboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaderboardService leaderboardService;

    @Test
    void shouldReturnGlobalLeaderboard() throws Exception {
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();

        when(leaderboardService.getGlobalLeaderboard())
                .thenReturn(List.of(
                        new LeaderboardEntryResponse(
                                1,
                                firstUserId,
                                "alice",
                                42
                        ),
                        new LeaderboardEntryResponse(
                                2,
                                secondUserId,
                                "bob",
                                27
                        )
                ));

        mockMvc.perform(
                        get("/api/leaderboard")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].userId").value(firstUserId.toString()))
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].totalPoints").value(42))
                .andExpect(jsonPath("$[1].rank").value(2))
                .andExpect(jsonPath("$[1].userId").value(secondUserId.toString()))
                .andExpect(jsonPath("$[1].username").value("bob"))
                .andExpect(jsonPath("$[1].totalPoints").value(27));
    }

    @Test
    void shouldReturnEmptyLeaderboard() throws Exception {
        when(leaderboardService.getGlobalLeaderboard())
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/leaderboard")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}