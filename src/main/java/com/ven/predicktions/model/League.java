package com.ven.predicktions.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "leagues",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_leagues_join_code", columnNames = "join_code")
        }
)
public class League {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "owner_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_leagues_owner")
    )
    private User owner;

    @Column(name = "join_code", nullable = false, length = 32)
    private String joinCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(
            mappedBy = "league",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private final List<LeagueMember> members = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    protected League() {
    }

    public League(String name, User owner, String joinCode) {
        this.name = name;
        this.owner = owner;
        this.joinCode = joinCode;
        addMember(owner);
    }

    public void addMember(User user) {
        members.add(new LeagueMember(this, user));
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public User getOwner() {
        return owner;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<LeagueMember> getMembers() {
        return List.copyOf(members);
    }
}
