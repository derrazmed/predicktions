package com.ven.predicktions.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "league_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_league_members_league_user",
                        columnNames = {"league_id", "user_id"}
                )
        }
)
public class LeagueMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "league_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_league_members_league")
    )
    private League league;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_league_members_user")
    )
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    protected LeagueMember() {
    }

    public LeagueMember(League league, User user) {
        this.league = league;
        this.user = user;
    }

    public UUID getId() {
        return id;
    }

    public League getLeague() {
        return league;
    }

    public User getUser() {
        return user;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
