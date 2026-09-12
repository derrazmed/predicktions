package com.ven.predicktions.scheduler;

import com.ven.predicktions.service.MatchSyncService;
import com.ven.predicktions.config.SportsSyncProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class MatchSyncScheduler {

    private final MatchSyncService matchSyncService;
    private final SportsSyncProperties properties;

    public MatchSyncScheduler(
            MatchSyncService matchSyncService,
            SportsSyncProperties properties
    ) {
        this.matchSyncService = matchSyncService;
        this.properties = properties;
    }

    @Scheduled(
            cron = "${sports.sync.fixtures-cron}",
            zone = "Africa/Casablanca"
    )
    public void synchronizeFixtures() {
        matchSyncService.synchronizeFixtures(
                properties.getCompetitionCode()
        );
    }

    @Scheduled(
            cron = "${sports.sync.results-cron}",
            zone = "Africa/Casablanca"
    )
    public void synchronizeResults() {

        LocalDate today = LocalDate.now(
                ZoneId.of("Africa/Casablanca")
        );

        properties.getResultSchedules().stream()
                .filter(schedule -> schedule.getDate().equals(today))
                .forEach(schedule ->
                        matchSyncService.synchronizeResults(
                                properties.getCompetitionCode(),
                                schedule.getMatchday()
                        )
                );
    }
}