package com.ven.predicktions.scheduler;

import java.time.LocalDate;
import java.util.Map;

public class MatchSyncSchedule {

    private final LocalDate fixtureSyncDate;
    private final Map<LocalDate, Integer> resultSyncDates;

    public MatchSyncSchedule(
            LocalDate fixtureSyncDate,
            Map<LocalDate, Integer> resultSyncDates
    ) {
        this.fixtureSyncDate = fixtureSyncDate;
        this.resultSyncDates = Map.copyOf(resultSyncDates);
    }

    public LocalDate getFixtureSyncDate() {
        return fixtureSyncDate;
    }

    public Map<LocalDate, Integer> getResultSyncDates() {
        return resultSyncDates;
    }
}