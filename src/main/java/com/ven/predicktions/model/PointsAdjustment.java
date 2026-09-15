package com.ven.predicktions.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "points_adjustments",
        indexes = {
                @Index(name = "idx_points_adjustments_user_id", columnList = "user_id"),
                @Index(name = "idx_points_adjustments_created_at", columnList = "created_at")
        }
)
public class PointsAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_points_adjustments_user")
    )
    private User user;

    @Column(nullable = false)
    private Integer points;

    @Column(nullable = false, length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "awarded_by",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_points_adjustments_awarded_by")
    )
    private User awardedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PointsAdjustment() {
    }

    public PointsAdjustment(User user, int points, String reason, User awardedBy) {
        this.user = user;
        this.points = points;
        this.reason = reason;
        this.awardedBy = awardedBy;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Integer getPoints() {
        return points;
    }

    public String getReason() {
        return reason;
    }

    public User getAwardedBy() {
        return awardedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
