package com.priceforge.sharedkernel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * The priority/tie-break algorithm both Pricing (UC-07) and Promotions (UC-10/11) use to decide
 * which of several candidates actually apply: highest priority first; an EXCLUSIVE item excludes
 * every lower-priority candidate; two items tied at the same priority are only incompatible if at
 * least one is EXCLUSIVE (two STACKABLE ties both apply); when a tie is incompatible, EXCLUSIVE
 * beats STACKABLE, and between two tied EXCLUSIVE items the earliest-created one wins.
 *
 * <p>Deliberately stops at "which items win, grouped by simultaneous application" — it knows
 * nothing about discounts, money, or how a group's items combine numerically. Each context is
 * still responsible for applying its own effect (see Pricing's {@code PriceCalculatorService}),
 * since two STACKABLE items tied at the same priority must be computed against the same starting
 * point rather than chained, or the "order doesn't matter" guarantee wouldn't hold for a mix of
 * percentage and fixed-amount effects.
 */
public final class ConflictResolver {

    private ConflictResolver() {
    }

    public static <T extends ConflictResolvable<ID>, ID> ConflictResolution<T, ID> resolve(List<T> candidates) {
        Map<Integer, List<T>> byPriorityDescending = candidates.stream()
            .collect(Collectors.groupingBy(
                (T item) -> item.priority(),
                () -> new TreeMap<Integer, List<T>>(Comparator.<Integer>reverseOrder()),
                Collectors.toList()));

        List<ResolvedGroup<T>> groupsToApply = new ArrayList<>();
        List<ExcludedItem<ID>> excluded = new ArrayList<>();
        boolean exclusiveApplied = false;

        for (List<T> group : byPriorityDescending.values()) {
            if (exclusiveApplied) {
                for (T item : group) {
                    excluded.add(new ExcludedItem<>(item.conflictId(), ConflictExclusionReason.EXCLUDED_BY_HIGHER_PRIORITY));
                }
                continue;
            }

            List<T> exclusiveInGroup = group.stream()
                .filter(item -> item.combinationMode() == CombinationMode.EXCLUSIVE)
                .toList();

            if (exclusiveInGroup.isEmpty()) {
                groupsToApply.add(new ResolvedGroup<>(group));
                continue;
            }

            T winner = exclusiveInGroup.size() == 1
                ? exclusiveInGroup.get(0)
                : exclusiveInGroup.stream().min(Comparator.comparing(T::createdAt)).orElseThrow();

            for (T item : group) {
                if (!item.conflictId().equals(winner.conflictId())) {
                    excluded.add(new ExcludedItem<>(item.conflictId(), ConflictExclusionReason.EXCLUDED_BY_TIEBREAK));
                }
            }

            groupsToApply.add(new ResolvedGroup<>(List.of(winner)));
            exclusiveApplied = true;
        }

        return new ConflictResolution<>(groupsToApply, excluded);
    }
}
