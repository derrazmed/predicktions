package com.ven.predicktions.integration.sports.footballdata;

import java.time.LocalDate;

public record FootballDataSeason(
        Integer id,
        LocalDate startDate,
        LocalDate endDate,
        Integer currentMatchday
) {
}
