package com.ven.predicktions.service.impl;

import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.model.League;
import com.ven.predicktions.repository.LeagueMemberRepository;
import com.ven.predicktions.repository.LeagueRepository;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.AdminLeagueMembershipService;
import com.ven.predicktions.service.AdminAuditService;
import com.ven.predicktions.model.AdminAuditAction;
import com.ven.predicktions.model.AdminAuditTargetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Service
public class AdminLeagueMembershipServiceImpl implements AdminLeagueMembershipService {

    private static final Logger log =
            LoggerFactory.getLogger(AdminLeagueMembershipServiceImpl.class);

    private final LeagueRepository leagueRepository;
    private final LeagueMemberRepository leagueMemberRepository;
    private final UserRepository userRepository;
    private final AdminAuditService auditService;

    public AdminLeagueMembershipServiceImpl(
            LeagueRepository leagueRepository,
            LeagueMemberRepository leagueMemberRepository,
            UserRepository userRepository
    ) {
        this(leagueRepository, leagueMemberRepository, userRepository, null);
    }
    @Autowired
    public AdminLeagueMembershipServiceImpl(LeagueRepository leagueRepository,
            LeagueMemberRepository leagueMemberRepository, UserRepository userRepository,
            AdminAuditService auditService) {
        this.leagueRepository = leagueRepository;
        this.leagueMemberRepository = leagueMemberRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public void removeMember(UUID leagueId, UUID userId, UUID adminId) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new ResourceNotFoundException("League not found"));

        var removedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (league.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Cannot remove the league owner.");
        }

        var membership = leagueMemberRepository.findByLeagueIdAndUserId(leagueId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

        leagueMemberRepository.delete(membership);
        if (auditService != null) {
            auditService.record(adminId, AdminAuditAction.LEAGUE_MEMBER_REMOVED,
                    AdminAuditTargetType.USER, userId,
                    "Removed user " + removedUser.getUsername() + " from league " + league.getName());
        }
        log.info("Admin {} removed user {} from league {}", adminId, userId, leagueId);
    }
}
