package com.ven.predicktions.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "predictions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_predictions_user_match",
                        columnNames = {"user_id", "match_id"}
                )
        }
)
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_predictions_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "match_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_predictions_match")
    )
    private Match match;

    @Column(name = "predicted_home_score", nullable = false)
    private Integer predictedHomeScore;

    @Column(name = "predicted_away_score", nullable = false)
    private Integer predictedAwayScore;

    @Column(nullable = false)
    private Integer points;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    protected Prediction() {
    }

    public Prediction(
            User user,
            Match match,
            Integer predictedHomeScore,
            Integer predictedAwayScore
    ) {
        this.user = user;
        this.match = match;
        this.predictedHomeScore = predictedHomeScore;
        this.predictedAwayScore = predictedAwayScore;
        this.points = 0;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Match getMatch() {
        return match;
    }

    public Integer getPredictedHomeScore() {
        return predictedHomeScore;
    }

    public Integer getPredictedAwayScore() {
        return predictedAwayScore;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}