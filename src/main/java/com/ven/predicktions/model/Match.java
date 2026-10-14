package com.ven.predicktions.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "matches",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_matches_external_id", columnNames = "external_id")
        }
)
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @Column(name = "home_team", nullable = false, length = 100)
    private String homeTeam;

    @Column(name = "away_team", nullable = false, length = 100)
    private String awayTeam;

    @Column(name = "kickoff_at", nullable = false)
    private Instant kickoffAt;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status;

    @Column(name = "competition_code", length = 20)
    private String competitionCode;

    @Column(name = "season")
    private Integer season;

    @Column(name = "matchday")
    private Integer matchday;

    protected Match() {
    }

    public Match(
            String externalId,
            String homeTeam,
            String awayTeam,
            Instant kickoffAt,
            MatchStatus status
    ) {
        this(externalId, homeTeam, awayTeam, kickoffAt, status, null, null, null);
    }

    public Match(
            String externalId,
            String homeTeam,
            String awayTeam,
            Instant kickoffAt,
            MatchStatus status,
            String competitionCode,
            Integer season,
            Integer matchday
    ) {
        this.externalId = externalId;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.kickoffAt = kickoffAt;
        this.status = status;
        this.competitionCode = competitionCode;
        this.season = season;
        this.matchday = matchday;
    }

    public UUID getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getHomeTeam() {
        return homeTeam;
    }

    public String getAwayTeam() {
        return awayTeam;
    }

    public Instant getKickoffAt() {
        return kickoffAt;
    }

    public Integer getHomeScore() {
        return homeScore;
    }

    public void setHomeScore(Integer homeScore) {
        this.homeScore = homeScore;
    }

    public Integer getAwayScore() {
        return awayScore;
    }

    public void setAwayScore(Integer awayScore) {
        this.awayScore = awayScore;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public String getCompetitionCode() {
        return competitionCode;
    }

    public void setCompetitionCode(String competitionCode) {
        this.competitionCode = competitionCode;
    }

    public Integer getSeason() {
        return season;
    }

    public void setSeason(Integer season) {
        this.season = season;
    }

    public Integer getMatchday() {
        return matchday;
    }

    public void setMatchday(Integer matchday) {
        this.matchday = matchday;
    }

    public void updateFrom(
            String homeTeam,
            String awayTeam,
            Instant kickoffAt,
            Integer homeScore,
            Integer awayScore,
            MatchStatus status
    ) {
        updateFrom(homeTeam, awayTeam, kickoffAt, homeScore, awayScore, status,
                this.competitionCode, this.season, this.matchday);
    }

    public void updateFrom(
            String homeTeam,
            String awayTeam,
            Instant kickoffAt,
            Integer homeScore,
            Integer awayScore,
            MatchStatus status,
            String competitionCode,
            Integer season,
            Integer matchday
    ) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.kickoffAt = kickoffAt;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.status = status;
        this.competitionCode = competitionCode;
        this.season = season;
        this.matchday = matchday;
    }
}
