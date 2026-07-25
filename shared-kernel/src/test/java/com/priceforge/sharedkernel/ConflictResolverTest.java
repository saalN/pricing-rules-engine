package com.priceforge.sharedkernel;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConflictResolverTest {

    private record Candidate(String id, int priority, CombinationMode combinationMode, Instant createdAt)
        implements ConflictResolvable<String> {

        @Override
        public String conflictId() {
            return id;
        }
    }

    private static Candidate candidate(String id, int priority, CombinationMode mode) {
        return new Candidate(id, priority, mode, Instant.now());
    }

    @Test
    void aSingleStackableCandidateFormsItsOwnGroupAndIsNotExcluded() {
        Candidate a = candidate("a", 10, CombinationMode.STACKABLE);

        ConflictResolution<Candidate, String> result = ConflictResolver.resolve(List.of(a));

        assertThat(result.groupsToApply()).hasSize(1);
        assertThat(result.groupsToApply().get(0).items()).containsExactly(a);
        assertThat(result.excluded()).isEmpty();
    }

    @Test
    void exclusiveExcludesLowerPriorityCandidatesByHigherPriorityNotTiebreak() {
        Candidate high = candidate("high", 20, CombinationMode.EXCLUSIVE);
        Candidate low = candidate("low", 10, CombinationMode.STACKABLE);

        ConflictResolution<Candidate, String> result = ConflictResolver.resolve(List.of(low, high));

        assertThat(result.groupsToApply()).hasSize(1);
        assertThat(result.groupsToApply().get(0).items()).containsExactly(high);
        assertThat(result.excluded()).containsExactly(
            new ExcludedItem<>("low", ConflictExclusionReason.EXCLUDED_BY_HIGHER_PRIORITY));
    }

    @Test
    void twoStackableCandidatesTiedAtSamePriorityFormOneGroupAndAreNotExcluded() {
        Candidate a = candidate("a", 10, CombinationMode.STACKABLE);
        Candidate b = candidate("b", 10, CombinationMode.STACKABLE);

        ConflictResolution<Candidate, String> result = ConflictResolver.resolve(List.of(a, b));

        assertThat(result.groupsToApply()).hasSize(1);
        assertThat(result.groupsToApply().get(0).items()).containsExactlyInAnyOrder(a, b);
        assertThat(result.excluded()).isEmpty();
    }

    @Test
    void twoExclusiveCandidatesTiedAtSamePriorityOnlyTheEarliestCreatedWins() {
        Instant earlier = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant later = Instant.now();
        Candidate earlierCandidate = new Candidate("earlier", 10, CombinationMode.EXCLUSIVE, earlier);
        Candidate laterCandidate = new Candidate("later", 10, CombinationMode.EXCLUSIVE, later);

        ConflictResolution<Candidate, String> result =
            ConflictResolver.resolve(List.of(laterCandidate, earlierCandidate));

        assertThat(result.groupsToApply()).hasSize(1);
        assertThat(result.groupsToApply().get(0).items()).containsExactly(earlierCandidate);
        assertThat(result.excluded()).containsExactly(
            new ExcludedItem<>("later", ConflictExclusionReason.EXCLUDED_BY_TIEBREAK));
    }

    @Test
    void exclusiveBeatsStackableWhenTiedAtSamePriority() {
        Candidate exclusive = candidate("exclusive", 10, CombinationMode.EXCLUSIVE);
        Candidate stackable = candidate("stackable", 10, CombinationMode.STACKABLE);

        ConflictResolution<Candidate, String> result = ConflictResolver.resolve(List.of(stackable, exclusive));

        assertThat(result.groupsToApply().get(0).items()).containsExactly(exclusive);
        assertThat(result.excluded()).containsExactly(
            new ExcludedItem<>("stackable", ConflictExclusionReason.EXCLUDED_BY_TIEBREAK));
    }

    @Test
    void multiplePriorityLevelsProduceGroupsInDescendingPriorityOrder() {
        Candidate high = candidate("high", 20, CombinationMode.STACKABLE);
        Candidate low = candidate("low", 10, CombinationMode.STACKABLE);

        ConflictResolution<Candidate, String> result = ConflictResolver.resolve(List.of(low, high));

        assertThat(result.groupsToApply()).hasSize(2);
        assertThat(result.groupsToApply().get(0).items()).containsExactly(high);
        assertThat(result.groupsToApply().get(1).items()).containsExactly(low);
    }
}
