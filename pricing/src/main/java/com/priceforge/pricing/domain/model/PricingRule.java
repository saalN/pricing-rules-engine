package com.priceforge.pricing.domain.model;

import com.priceforge.pricing.domain.exception.PricingRuleNotActiveException;
import com.priceforge.sharedkernel.PricingRuleId;
import java.time.Instant;
import java.util.Objects;

/**
 * A pricing rule: a condition that gates it, a discount it applies, a priority and combination
 * mode used to resolve conflicts with other rules (UC-07), and a scope determining which products
 * it can apply to.
 */
public final class PricingRule {

    private final PricingRuleId id;
    private final PricingRuleType type;
    private RuleCondition condition;
    private final RuleScope scope;
    private Discount discount;
    private int priority;
    private CombinationMode combinationMode;
    private PricingRuleStatus status;
    private final Instant createdAt;

    private PricingRule(
            PricingRuleId id,
            PricingRuleType type,
            RuleCondition condition,
            RuleScope scope,
            Discount discount,
            int priority,
            CombinationMode combinationMode,
            PricingRuleStatus status,
            Instant createdAt) {
        this.id = id;
        this.type = type;
        this.condition = condition;
        this.scope = scope;
        this.discount = discount;
        this.priority = priority;
        this.combinationMode = combinationMode;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static PricingRule create(
            PricingRuleId id,
            PricingRuleType type,
            RuleCondition condition,
            RuleScope scope,
            Discount discount,
            int priority,
            CombinationMode combinationMode,
            Instant createdAt) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(condition, "condition must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(discount, "discount must not be null");
        Objects.requireNonNull(combinationMode, "combinationMode must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");

        return new PricingRule(
            id, type, condition, scope, discount, priority, combinationMode, PricingRuleStatus.ACTIVE, createdAt);
    }

    public static PricingRule reconstitute(
            PricingRuleId id,
            PricingRuleType type,
            RuleCondition condition,
            RuleScope scope,
            Discount discount,
            int priority,
            CombinationMode combinationMode,
            PricingRuleStatus status,
            Instant createdAt) {
        return new PricingRule(id, type, condition, scope, discount, priority, combinationMode, status, createdAt);
    }

    /**
     * UC-09: type and scope are not modifiable — changing what fundamentally triggers a rule or
     * what it applies to is a new rule, not an edit of this one.
     */
    public void update(RuleCondition newCondition, Discount newDiscount, int newPriority, CombinationMode newCombinationMode) {
        requireActive();
        Objects.requireNonNull(newCondition, "newCondition must not be null");
        Objects.requireNonNull(newDiscount, "newDiscount must not be null");
        Objects.requireNonNull(newCombinationMode, "newCombinationMode must not be null");
        this.condition = newCondition;
        this.discount = newDiscount;
        this.priority = newPriority;
        this.combinationMode = newCombinationMode;
    }

    /** UC-09: rules are never deleted, only deactivated — the history stays immutable. */
    public void deactivate() {
        requireActive();
        this.status = PricingRuleStatus.INACTIVE;
    }

    private void requireActive() {
        if (status != PricingRuleStatus.ACTIVE) {
            throw new PricingRuleNotActiveException(id);
        }
    }

    /** Compact summary used for the previous/new state pair recorded by UC-09's audit trail. */
    public String describeState() {
        return "priority=" + priority
            + ", combinationMode=" + combinationMode
            + ", discountType=" + discount.type()
            + ", discountValue=" + discount.value()
            + ", condition=" + condition.describe();
    }

    public PricingRuleId id() {
        return id;
    }

    public PricingRuleType type() {
        return type;
    }

    public RuleCondition condition() {
        return condition;
    }

    public RuleScope scope() {
        return scope;
    }

    public Discount discount() {
        return discount;
    }

    public int priority() {
        return priority;
    }

    public CombinationMode combinationMode() {
        return combinationMode;
    }

    public PricingRuleStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
