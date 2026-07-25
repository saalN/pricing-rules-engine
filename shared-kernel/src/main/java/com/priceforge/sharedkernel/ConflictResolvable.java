package com.priceforge.sharedkernel;

import java.time.Instant;

/**
 * Contract required by {@link ConflictResolver}: anything that competes for priority against
 * peers of its own kind (a Pricing rule against other rules, a Promotion against other
 * promotions) exposes just enough to resolve conflicts — nothing about what it actually does.
 */
public interface ConflictResolvable<ID> {

    ID conflictId();

    int priority();

    CombinationMode combinationMode();

    Instant createdAt();
}
