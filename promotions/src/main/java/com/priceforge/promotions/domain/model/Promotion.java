package com.priceforge.promotions.domain.model;

import com.priceforge.sharedkernel.CustomerSegment;
import com.priceforge.promotions.domain.exception.InvalidPromotionStateTransitionException;
import com.priceforge.promotions.domain.exception.InvalidPromotionValidityWindowException;
import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.sharedkernel.ConflictResolvable;
import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.PromotionId;
import java.time.Instant;
import java.util.Objects;

/**
 * A promotional campaign (UC-10): a validity window, per-global/per-user usage limits,
 * eligibility conditions, a discount effect, and a priority/combinationMode used to resolve
 * conflicts with other promotions in the same calculation (UC-11, via shared-kernel's generic
 * ConflictResolver — the same algorithm PricingRule uses for UC-07).
 *
 * <p>Usage-limit tracking is deliberately NOT modeled as in-memory state on this aggregate: the
 * global and per-user counters must be checked and incremented atomically under concurrent
 * requests, which only a single SQL statement in the persistence adapter can guarantee. This
 * aggregate only knows the configured limits and its own SCHEDULED/ACTIVE/EXHAUSTED/EXPIRED
 * lifecycle; {@link #exhaust()} is called by the application layer after the repository's atomic
 * operation reports the global limit was just reached.
 */
public final class Promotion implements ConflictResolvable<PromotionId> {

    private final PromotionId id;
    private final String name;
    private final String description;
    private final Instant validFrom;
    private final Instant validUntil;
    private final int globalUsageLimit;
    private final int perUserUsageLimit;
    private final EligibilityCriteria eligibility;
    private Discount discount;
    private int priority;
    private CombinationMode combinationMode;
    private PromotionStatus status;
    private final Instant createdAt;

    private Promotion(
            PromotionId id,
            String name,
            String description,
            Instant validFrom,
            Instant validUntil,
            int globalUsageLimit,
            int perUserUsageLimit,
            EligibilityCriteria eligibility,
            Discount discount,
            int priority,
            CombinationMode combinationMode,
            PromotionStatus status,
            Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.globalUsageLimit = globalUsageLimit;
        this.perUserUsageLimit = perUserUsageLimit;
        this.eligibility = eligibility;
        this.discount = discount;
        this.priority = priority;
        this.combinationMode = combinationMode;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Promotion create(
            PromotionId id,
            String name,
            String description,
            Instant validFrom,
            Instant validUntil,
            int globalUsageLimit,
            int perUserUsageLimit,
            EligibilityCriteria eligibility,
            Discount discount,
            int priority,
            CombinationMode combinationMode,
            Instant now) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(validFrom, "validFrom must not be null");
        Objects.requireNonNull(validUntil, "validUntil must not be null");
        Objects.requireNonNull(eligibility, "eligibility must not be null");
        Objects.requireNonNull(discount, "discount must not be null");
        Objects.requireNonNull(combinationMode, "combinationMode must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (!validFrom.isBefore(validUntil)) {
            throw new InvalidPromotionValidityWindowException(
                "validFrom (" + validFrom + ") must be before validUntil (" + validUntil + ")");
        }
        if (globalUsageLimit <= 0) {
            throw new IllegalArgumentException("globalUsageLimit must be positive, but was " + globalUsageLimit);
        }
        if (perUserUsageLimit <= 0) {
            throw new IllegalArgumentException("perUserUsageLimit must be positive, but was " + perUserUsageLimit);
        }

        PromotionStatus initialStatus = now.isBefore(validFrom) ? PromotionStatus.SCHEDULED : PromotionStatus.ACTIVE;
        return new Promotion(
            id, name, description, validFrom, validUntil, globalUsageLimit, perUserUsageLimit, eligibility,
            discount, priority, combinationMode, initialStatus, now);
    }

    public static Promotion reconstitute(
            PromotionId id,
            String name,
            String description,
            Instant validFrom,
            Instant validUntil,
            int globalUsageLimit,
            int perUserUsageLimit,
            EligibilityCriteria eligibility,
            Discount discount,
            int priority,
            CombinationMode combinationMode,
            PromotionStatus status,
            Instant createdAt) {
        return new Promotion(
            id, name, description, validFrom, validUntil, globalUsageLimit, perUserUsageLimit, eligibility,
            discount, priority, combinationMode, status, createdAt);
    }

    /**
     * Real-time validity check (UC-12's own suggestion): a SCHEDULED promotion whose
     * {@code validFrom} has already passed is treated as within-window even before the
     * once-a-minute scheduler flips its persisted status, and an ACTIVE promotion past
     * {@code validUntil} is treated as out-of-window even before the scheduler expires it —
     * bounding the up-to-a-minute lag the scheduler's granularity would otherwise allow.
     */
    public boolean isWithinValidityWindow(Instant now) {
        return !now.isBefore(validFrom) && now.isBefore(validUntil);
    }

    /** UC-11 step 2: eligibility alone, independent of usage-limit checking (a later, atomic step). */
    public boolean isEligible(CustomerSegment contextSegment, String contextCategory, Money currentPrice, Instant now) {
        if (status == PromotionStatus.EXPIRED || status == PromotionStatus.EXHAUSTED) {
            return false;
        }
        if (!isWithinValidityWindow(now)) {
            return false;
        }
        return eligibility.matches(contextSegment, contextCategory, currentPrice);
    }

    /** UC-12: SCHEDULED -> ACTIVE, driven by the scheduler reaching validFrom. */
    public void activate() {
        if (status != PromotionStatus.SCHEDULED) {
            throw new InvalidPromotionStateTransitionException(id, status, "activate");
        }
        this.status = PromotionStatus.ACTIVE;
    }

    /** UC-12: ACTIVE -> EXPIRED, driven by the scheduler passing validUntil. Never reversible. */
    public void expire() {
        if (status != PromotionStatus.ACTIVE) {
            throw new InvalidPromotionStateTransitionException(id, status, "expire");
        }
        this.status = PromotionStatus.EXPIRED;
    }

    /** UC-11: ACTIVE -> EXHAUSTED, driven by usage (not time) — a distinct terminal status from EXPIRED. */
    public void exhaust() {
        if (status != PromotionStatus.ACTIVE) {
            throw new InvalidPromotionStateTransitionException(id, status, "exhaust");
        }
        this.status = PromotionStatus.EXHAUSTED;
    }

    public PromotionId id() {
        return id;
    }

    @Override
    public PromotionId conflictId() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Instant validFrom() {
        return validFrom;
    }

    public Instant validUntil() {
        return validUntil;
    }

    public int globalUsageLimit() {
        return globalUsageLimit;
    }

    public int perUserUsageLimit() {
        return perUserUsageLimit;
    }

    public EligibilityCriteria eligibility() {
        return eligibility;
    }

    public Discount discount() {
        return discount;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public CombinationMode combinationMode() {
        return combinationMode;
    }

    public PromotionStatus status() {
        return status;
    }

    @Override
    public Instant createdAt() {
        return createdAt;
    }
}
