package com.ven.predicktions.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "sports.sync")
public class SportsSyncProperties {

    private String competitionCode;

    private String fixturesCron;

    private String resultsCron;

    private List<ResultSchedule> resultSchedules = new ArrayList<>();

    public String getCompetitionCode() {
        return competitionCode;
    }

    public void setCompetitionCode(String competitionCode) {
        this.competitionCode = competitionCode;
    }

    public String getFixturesCron() {
        return fixturesCron;
    }

    public void setFixturesCron(String fixturesCron) {
        this.fixturesCron = fixturesCron;
    }

    public String getResultsCron() {
        return resultsCron;
    }

    public void setResultsCron(String resultsCron) {
        this.resultsCron = resultsCron;
    }

    public List<ResultSchedule> getResultSchedules() {
        return resultSchedules;
    }

    public void setResultSchedules(List<ResultSchedule> resultSchedules) {
        this.resultSchedules = resultSchedules;
    }

    public static class ResultSchedule {

        private Integer matchday;
        private LocalDate date;

        public Integer getMatchday() {
            return matchday;
        }

        public void setMatchday(Integer matchday) {
            this.matchday = matchday;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }
    }
}