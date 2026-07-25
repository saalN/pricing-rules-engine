package com.priceforge.promotions.domain.model;

/**
 * Result of the persistence adapter's single atomic check-and-increment operation (UC-11): the
 * global and per-user counters are read, checked against their limits and incremented together,
 * so a concurrent request can never slip past a limit that was reached a moment earlier.
 */
public enum UsageRecordingOutcome {
    APPLIED,
    APPLIED_AND_NOW_EXHAUSTED,
    GLOBAL_LIMIT_REACHED,
    PER_USER_LIMIT_REACHED
}
