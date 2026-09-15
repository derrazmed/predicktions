package com.ven.predicktions.service;

import com.ven.predicktions.dto.league.AdminLeaguePageResponse;
import com.ven.predicktions.repository.AdminLeagueProjection;
import com.ven.predicktions.repository.LeagueRepository;
import com.ven.predicktions.service.impl.AdminLeagueServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminLeagueServiceTest {

    @Mock
    private LeagueRepository leagueRepository;

    @Mock
    private AdminLeagueProjection projection;

    @Test
    void shouldMapProjectionAndPreservePagination() {
        UUID leagueId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(projection.getId()).thenReturn(leagueId);
        when(projection.getName()).thenReturn("League");
        when(projection.getOwnerId()).thenReturn(ownerId);
        when(projection.getOwnerUsername()).thenReturn("owner");
        when(projection.getMemberCount()).thenReturn(2L);
        when(projection.getCreatedAt()).thenReturn(Instant.parse("2026-09-12T23:24:18Z"));
        when(projection.getJoinCode()).thenReturn("JOIN123");
        when(leagueRepository.findAllForAdministration(
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(new PageImpl<>(List.of(projection),
                org.springframework.data.domain.PageRequest.of(1, 20), 21));

        AdminLeaguePageResponse response =
                new AdminLeagueServiceImpl(leagueRepository).getLeagues(1, 20);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(21);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.content().getFirst().owner().id()).isEqualTo(ownerId);
        assertThat(response.content().getFirst().owner().username()).isEqualTo("owner");
        assertThat(response.content().getFirst().memberCount()).isEqualTo(2);
        assertThat(response.content().getFirst().joinCode()).isEqualTo("JOIN123");
        verify(leagueRepository).findAllForAdministration(
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void shouldRejectInvalidPagination() {
        AdminLeagueService service = new AdminLeagueServiceImpl(leagueRepository);

        assertThatThrownBy(() -> service.getLeagues(-1, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getLeagues(0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getLeagues(0, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
