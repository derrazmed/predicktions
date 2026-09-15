package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.prediction.AdminPredictionPageResponse;
import com.ven.predicktions.dto.prediction.AdminPredictionResponse;
import com.ven.predicktions.model.Prediction;
import com.ven.predicktions.repository.MatchRepository;
import com.ven.predicktions.repository.PredictionRepository;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.AdminPredictionService;
import com.ven.predicktions.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AdminPredictionServiceImpl implements AdminPredictionService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PredictionRepository predictionRepository;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;

    public AdminPredictionServiceImpl(
            PredictionRepository predictionRepository,
            UserRepository userRepository,
            MatchRepository matchRepository
    ) {
        this.predictionRepository = predictionRepository;
        this.userRepository = userRepository;
        this.matchRepository = matchRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPredictionPageResponse getUserPredictions(
            UUID userId,
            int page,
            int size
    ) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        return getPredictions(
                userId, null, null, null, null, null, page, size
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPredictionPageResponse getMatchPredictions(
            UUID matchId,
            int page,
            int size
    ) {
        if (!matchRepository.existsById(matchId)) {
            throw new ResourceNotFoundException("Match not found");
        }

        return getPredictions(
                null, matchId, null, null, null, null, page, size
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPredictionPageResponse getPredictions(
            UUID userId,
            UUID matchId,
            Integer gameweek,
            UUID leagueId,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    ) {
        validate(page, size, gameweek, fromDate, toDate);

        Page<Prediction> predictions = predictionRepository.findAll(
                specification(userId, matchId, gameweek, leagueId, fromDate, toDate),
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.desc("createdAt"),
                                Sort.Order.desc("id")
                        )
                )
        );

        return new AdminPredictionPageResponse(
                predictions.getContent().stream()
                        .map(this::toResponse)
                        .toList(),
                predictions.getNumber(),
                predictions.getSize(),
                predictions.getTotalElements(),
                predictions.getTotalPages()
        );
    }

    private Specification<Prediction> specification(
            UUID userId,
            UUID matchId,
            Integer gameweek,
            UUID leagueId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return (root, query, criteriaBuilder) -> {
            Join<Object, Object> user = root.join("user", JoinType.INNER);
            Join<Object, Object> match = root.join("match", JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(criteriaBuilder.equal(user.get("id"), userId));
            }
            if (matchId != null) {
                predicates.add(criteriaBuilder.equal(match.get("id"), matchId));
            }
            if (gameweek != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.function(
                                "date_part",
                                Integer.class,
                                criteriaBuilder.literal("week"),
                                match.get("kickoffAt")
                        ),
                        gameweek
                ));
            }
            if (leagueId != null) {
                var membership = query.subquery(UUID.class);
                var member = membership.from(
                        com.ven.predicktions.model.LeagueMember.class
                );
                membership.select(member.get("id"));
                membership.where(
                        criteriaBuilder.equal(member.get("league").get("id"), leagueId),
                        criteriaBuilder.equal(member.get("user").get("id"), user.get("id"))
                );
                predicates.add(criteriaBuilder.exists(membership));
            }
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        fromDate.atStartOfDay().toInstant(ZoneOffset.UTC)
                ));
            }
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThan(
                        root.get("createdAt"),
                        toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
                ));
            }

            query.distinct(true);
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void validate(
            int page,
            int size,
            Integer gameweek,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }
        if (gameweek != null && (gameweek < 1 || gameweek > 53)) {
            throw new IllegalArgumentException("Gameweek must be between 1 and 53");
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate must not be after toDate");
        }
    }

    private AdminPredictionResponse toResponse(Prediction prediction) {
        return new AdminPredictionResponse(
                prediction.getId(),
                prediction.getUser().getId(),
                prediction.getUser().getUsername(),
                prediction.getMatch().getId(),
                prediction.getMatch().getKickoffAt()
                        .atZone(ZoneOffset.UTC)
                        .get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR),
                prediction.getPredictedHomeScore(),
                prediction.getPredictedAwayScore(),
                prediction.getPoints(),
                prediction.getCreatedAt(),
                prediction.getUpdatedAt()
        );
    }
}
