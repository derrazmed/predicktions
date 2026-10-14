package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.comparison.ComparisonMatchResponse;
import com.ven.predicktions.dto.comparison.ComparisonMemberResponse;
import com.ven.predicktions.dto.comparison.GameweekComparisonResponse;
import com.ven.predicktions.dto.comparison.GameweekSummaryResponse;
import com.ven.predicktions.dto.comparison.MemberPredictionResponse;
import com.ven.predicktions.dto.comparison.PredictionOutcome;
import com.ven.predicktions.exception.ForbiddenOperationException;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.model.League;
import com.ven.predicktions.model.LeagueMember;
import com.ven.predicktions.model.Match;
import com.ven.predicktions.model.MatchStatus;
import com.ven.predicktions.model.Prediction;
import com.ven.predicktions.repository.LeagueMemberRepository;
import com.ven.predicktions.repository.LeagueRepository;
import com.ven.predicktions.repository.MatchRepository;
import com.ven.predicktions.repository.PredictionRepository;
import com.ven.predicktions.service.GameweekCompletionService;
import com.ven.predicktions.service.LeagueComparisonService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Reveals league members' predictions per individual match.
 *
 * <p>A "gameweek" is the sports provider's matchday scoped to its competition
 * and season — never the ISO week of the kickoff date. A match appears as soon
 * as its final result is persisted and its predictions have been scored; the
 * rest of the gameweek may still be in progress.
 */
@Service
@Transactional(readOnly = true)
public class LeagueComparisonServiceImpl implements LeagueComparisonService {

    private final LeagueRepository leagueRepository;
    private final LeagueMemberRepository leagueMemberRepository;
    private final MatchRepository matchRepository;
    private final PredictionRepository predictionRepository;
    private final GameweekCompletionService completionService;

    public LeagueComparisonServiceImpl(
            LeagueRepository leagueRepository,
            LeagueMemberRepository leagueMemberRepository,
            MatchRepository matchRepository,
            PredictionRepository predictionRepository,
            GameweekCompletionService completionService
    ) {
        this.leagueRepository = leagueRepository;
        this.leagueMemberRepository = leagueMemberRepository;
        this.matchRepository = matchRepository;
        this.predictionRepository = predictionRepository;
        this.completionService = completionService;
    }

    @Override
    public List<GameweekSummaryResponse> getGameweeks(UUID userId, UUID leagueId) {
        findLeague(leagueId);
        requireMember(leagueId, userId);

        List<Match> allMatches = matchRepository.findAll();
        Map<GameweekKey, List<Match>> gameweeks = groupByGameweek(allMatches);
        Set<UUID> readyMatchIds = completionService.readyMatchIds(allMatches);

        return gameweeks.entrySet().stream()
                .sorted(Map.Entry.<GameweekKey, List<Match>>comparingByKey().reversed())
                .map(entry -> summarize(entry.getKey(), entry.getValue(), readyMatchIds))
                .toList();
    }

    @Override
    public GameweekComparisonResponse getGameweekComparison(
            UUID userId,
            UUID leagueId,
            String competition,
            int season,
            int gameweek,
            UUID memberId
    ) {
        League league = findLeague(leagueId);
        requireMember(leagueId, userId);

        GameweekKey key = new GameweekKey(competition, season, gameweek);

        List<Match> matches = groupByGameweek(matchRepository.findAll())
                .getOrDefault(key, List.of());

        if (matches.isEmpty()) {
            throw new ResourceNotFoundException("Gameweek not found");
        }

        Set<UUID> readyMatchIds = completionService.readyMatchIds(matches);
        List<Match> eligibleMatches = matches.stream()
                .filter(match -> readyMatchIds.contains(match.getId()))
                .toList();

        List<LeagueMember> members = leagueMemberRepository.findAllByLeagueIdOrdered(leagueId);

        if (memberId != null && members.stream().noneMatch(member -> member.getUser().getId().equals(memberId))) {
            throw new ResourceNotFoundException("League member not found");
        }

        List<LeagueMember> selectedMembers = memberId == null
                ? members
                : members.stream()
                        .filter(member -> member.getUser().getId().equals(memberId))
                        .toList();

        List<UUID> selectedMemberIds = selectedMembers.stream()
                .map(member -> member.getUser().getId())
                .toList();

        Map<UUID, Map<UUID, Prediction>> predictionsByMatchAndUser = loadPredictions(
                eligibleMatches, selectedMemberIds);

        List<ComparisonMemberResponse> memberResponses = selectedMembers.stream()
                .map(member -> new ComparisonMemberResponse(
                        member.getUser().getId(),
                        member.getUser().getUsername()))
                .toList();

        List<ComparisonMatchResponse> matchResponses = eligibleMatches.stream()
                .map(match -> toMatchResponse(match, selectedMembers, predictionsByMatchAndUser))
                .toList();

        GameweekSummaryResponse summary = summarize(key, matches, readyMatchIds);

        return new GameweekComparisonResponse(
                league.getId(),
                league.getName(),
                key.competition(),
                key.season(),
                key.gameweek(),
                summary.label(),
                summary.firstKickoffAt(),
                summary.lastKickoffAt(),
                memberResponses,
                matchResponses
        );
    }

    private Map<GameweekKey, List<Match>> groupByGameweek(List<Match> matches) {
        Map<GameweekKey, List<Match>> gameweeks = new HashMap<>();

        for (Match match : matches) {
            GameweekKey key = GameweekKey.of(match);

            if (key == null) {
                continue;
            }

            gameweeks.computeIfAbsent(key, ignored -> new ArrayList<>()).add(match);
        }

        gameweeks.values().forEach(group ->
                group.sort(Comparator.comparing(Match::getKickoffAt)));

        return gameweeks;
    }

    private Map<UUID, Map<UUID, Prediction>> loadPredictions(
            List<Match> matches,
            List<UUID> memberIds
    ) {
        Map<UUID, Map<UUID, Prediction>> predictionsByMatchAndUser = new HashMap<>();

        if (matches.isEmpty() || memberIds.isEmpty()) {
            return predictionsByMatchAndUser;
        }

        List<UUID> matchIds = matches.stream()
                .map(Match::getId)
                .toList();

        for (Prediction prediction : predictionRepository.findAllByMatchIdInAndUserIdIn(matchIds, memberIds)) {
            predictionsByMatchAndUser
                    .computeIfAbsent(prediction.getMatch().getId(), key -> new HashMap<>())
                    .put(prediction.getUser().getId(), prediction);
        }

        return predictionsByMatchAndUser;
    }

    private ComparisonMatchResponse toMatchResponse(
            Match match,
            List<LeagueMember> members,
            Map<UUID, Map<UUID, Prediction>> predictionsByMatchAndUser
    ) {
        Map<UUID, Prediction> predictionsByUser = predictionsByMatchAndUser
                .getOrDefault(match.getId(), Map.of());

        List<MemberPredictionResponse> predictions = members.stream()
                .map(member -> toMemberPrediction(match, member, predictionsByUser.get(member.getUser().getId())))
                .toList();

        return new ComparisonMatchResponse(
                match.getId(),
                match.getHomeTeam(),
                match.getAwayTeam(),
                match.getKickoffAt(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.getStatus(),
                predictions
        );
    }

    private MemberPredictionResponse toMemberPrediction(
            Match match,
            LeagueMember member,
            Prediction prediction
    ) {
        if (prediction == null) {
            return new MemberPredictionResponse(
                    member.getUser().getId(),
                    member.getUser().getUsername(),
                    null,
                    null,
                    null,
                    PredictionOutcome.NO_PREDICTION
            );
        }

        return new MemberPredictionResponse(
                member.getUser().getId(),
                member.getUser().getUsername(),
                prediction.getPredictedHomeScore(),
                prediction.getPredictedAwayScore(),
                prediction.getPoints(),
                resolveOutcome(match, prediction)
        );
    }

    private PredictionOutcome resolveOutcome(Match match, Prediction prediction) {
        if (match.getStatus() == MatchStatus.CANCELLED
                || match.getHomeScore() == null
                || match.getAwayScore() == null) {
            return PredictionOutcome.NO_RESULT;
        }

        int points = prediction.getPoints() == null ? 0 : prediction.getPoints();

        if (points == 3) {
            return PredictionOutcome.EXACT_SCORE;
        }

        if (points == 1) {
            return PredictionOutcome.CORRECT_OUTCOME;
        }

        return PredictionOutcome.INCORRECT;
    }

    private GameweekSummaryResponse summarize(
            GameweekKey key,
            List<Match> matches,
            Set<UUID> readyMatchIds
    ) {
        Instant firstKickoff = matches.stream()
                .map(Match::getKickoffAt)
                .min(Comparator.naturalOrder())
                .orElse(null);

        Instant lastKickoff = matches.stream()
                .map(Match::getKickoffAt)
                .max(Comparator.naturalOrder())
                .orElse(null);

        int eligibleMatchCount = (int) matches.stream()
                .filter(match -> readyMatchIds.contains(match.getId()))
                .count();

        return new GameweekSummaryResponse(
                key.competition(),
                key.season(),
                key.gameweek(),
                "Gameweek " + key.gameweek(),
                matches.size(),
                eligibleMatchCount,
                firstKickoff,
                lastKickoff
        );
    }

    private League findLeague(UUID leagueId) {
        return leagueRepository.findById(leagueId)
                .orElseThrow(() -> new ResourceNotFoundException("League not found"));
    }

    private void requireMember(UUID leagueId, UUID userId) {
        if (!leagueMemberRepository.existsByLeagueIdAndUserId(leagueId, userId)) {
            throw new ForbiddenOperationException("You are not a member of this league");
        }
    }

    /**
     * Identity of a gameweek: the provider competition plus the season and
     * matchday it belongs to. A match without this identity cannot be placed in
     * a gameweek and is therefore excluded rather than guessed from its date.
     */
    private record GameweekKey(
            String competition,
            int season,
            int gameweek
    ) implements Comparable<GameweekKey> {

        static GameweekKey of(Match match) {
            if (match == null
                    || match.getCompetitionCode() == null
                    || match.getSeason() == null
                    || match.getMatchday() == null) {
                return null;
            }

            return new GameweekKey(
                    match.getCompetitionCode(),
                    match.getSeason(),
                    match.getMatchday()
            );
        }

        @Override
        public int compareTo(GameweekKey other) {
            int seasonComparison = Integer.compare(season, other.season);

            if (seasonComparison != 0) {
                return seasonComparison;
            }

            int gameweekComparison = Integer.compare(gameweek, other.gameweek);

            if (gameweekComparison != 0) {
                return gameweekComparison;
            }

            return competition.compareTo(other.competition);
        }
    }
}
