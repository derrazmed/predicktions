package com.ven.predicktions.repository;

import com.ven.predicktions.model.League;
import com.ven.predicktions.model.LeagueMember;
import com.ven.predicktions.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class LeagueRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private LeagueRepository leagueRepository;

    @Autowired
    private LeagueMemberRepository leagueMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldPersistLeagueWithRequiredFields() {
        User owner = createUser("owner");

        League saved = leagueRepository.save(
                new League("Champions Club", owner, "JOIN123")
        );

        Optional<League> result = leagueRepository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isNotNull();
        assertThat(result.get().getName()).isEqualTo("Champions Club");
        assertThat(result.get().getOwner().getId()).isEqualTo(owner.getId());
        assertThat(result.get().getJoinCode()).isEqualTo("JOIN123");
        assertThat(result.get().getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindLeagueByJoinCode() {
        User owner = createUser("owner");
        leagueRepository.save(new League("Friends League", owner, "ABCXYZ"));

        Optional<League> result = leagueRepository.findByJoinCode("ABCXYZ");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Friends League");
        assertThat(leagueRepository.existsByJoinCode("ABCXYZ")).isTrue();
        assertThat(leagueRepository.existsByJoinCode("UNKNOWN")).isFalse();
    }

    @Test
    void shouldRejectDuplicateJoinCode() {
        User owner = createUser("owner");
        leagueRepository.saveAndFlush(new League("League One", owner, "SAMECODE"));

        User otherOwner = createUser("other-owner");

        assertThatThrownBy(() ->
                leagueRepository.saveAndFlush(
                        new League("League Two", otherOwner, "SAMECODE")
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAddOwnerAsMemberWhenLeagueIsCreated() {
        User owner = createUser("owner");
        League league = leagueRepository.save(
                new League("Owner League", owner, "OWNER01")
        );

        Optional<LeagueMember> membership =
                leagueMemberRepository.findByLeagueIdAndUserId(
                        league.getId(),
                        owner.getId()
                );

        assertThat(membership).isPresent();
        assertThat(membership.get().getUser().getId()).isEqualTo(owner.getId());
        assertThat(membership.get().getLeague().getId()).isEqualTo(league.getId());
        assertThat(membership.get().getCreatedAt()).isNotNull();
    }

    @Test
    void shouldAllowMultipleMembersInALeague() {
        User owner = createUser("owner");
        User member = createUser("member");

        League league = new League("Group League", owner, "GROUP01");
        league.addMember(member);
        leagueRepository.save(league);

        List<LeagueMember> members =
                leagueMemberRepository.findAllByLeagueId(league.getId());

        assertThat(members).hasSize(2);
        assertThat(members)
                .extracting(leagueMember -> leagueMember.getUser().getId())
                .containsExactlyInAnyOrder(owner.getId(), member.getId());
    }

    @Test
    void shouldAllowAUserToBelongToMultipleLeagues() {
        User user = createUser("shared-user");

        League firstLeague = new League("First", createUser("owner-1"), "FIRST01");
        firstLeague.addMember(user);
        leagueRepository.save(firstLeague);

        League secondLeague = new League("Second", createUser("owner-2"), "SECOND1");
        secondLeague.addMember(user);
        leagueRepository.save(secondLeague);

        List<LeagueMember> memberships =
                leagueMemberRepository.findAllByUserId(user.getId());

        assertThat(memberships).hasSize(2);
        assertThat(memberships)
                .extracting(leagueMember -> leagueMember.getLeague().getId())
                .containsExactlyInAnyOrder(firstLeague.getId(), secondLeague.getId());
    }

    @Test
    void shouldRejectDuplicateMembershipInTheSameLeague() {
        User owner = createUser("owner");
        User member = createUser("member");

        League league = new League("Unique Members", owner, "UNIQUE1");
        league.addMember(member);
        leagueRepository.saveAndFlush(league);

        assertThatThrownBy(() ->
                leagueMemberRepository.saveAndFlush(new LeagueMember(league, member))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    private User createUser(String username) {
        return userRepository.save(
                new User(
                        username,
                        username + "@example.com",
                        "hashed-password"
                )
        );
    }
}
