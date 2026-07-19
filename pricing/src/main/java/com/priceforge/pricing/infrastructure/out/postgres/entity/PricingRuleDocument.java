package com.priceforge.pricing.infrastructure.out.postgres.entity;

import com.priceforge.pricing.domain.model.CombinationMode;
import com.priceforge.pricing.domain.model.CustomerSegment;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.PricingRuleStatus;
import com.priceforge.pricing.domain.model.PricingRuleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

/**
 * A single table holds every rule type; only the columns matching {@code type} are populated —
 * simplest mapping for a small, closed set of condition shapes (see {@code RuleCondition}).
 */
@Entity
@Table(name = "pricing_rule", schema = "pricing")
public class PricingRuleDocument {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PricingRuleType type;

    @Column(name = "time_window_start")
    private LocalTime timeWindowStart;

    @Column(name = "time_window_end")
    private LocalTime timeWindowEnd;

    @Enumerated(EnumType.STRING)
    private CustomerSegment segment;

    @Column(name = "minimum_quantity")
    private Integer minimumQuantity;

    private String zone;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    private BigDecimal discountValue;

    @Column(nullable = false)
    private int priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "combination_mode", nullable = false)
    private CombinationMode combinationMode;

    @Column(name = "scope_product_id")
    private UUID scopeProductId;

    @Column(name = "scope_category")
    private String scopeCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PricingRuleStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PricingRuleDocument() {
        // required by JPA
    }

    public PricingRuleDocument(
            UUID id,
            PricingRuleType type,
            LocalTime timeWindowStart,
            LocalTime timeWindowEnd,
            CustomerSegment segment,
            Integer minimumQuantity,
            String zone,
            DiscountType discountType,
            BigDecimal discountValue,
            int priority,
            CombinationMode combinationMode,
            UUID scopeProductId,
            String scopeCategory,
            PricingRuleStatus status,
            Instant createdAt) {
        this.id = id;
        this.type = type;
        this.timeWindowStart = timeWindowStart;
        this.timeWindowEnd = timeWindowEnd;
        this.segment = segment;
        this.minimumQuantity = minimumQuantity;
        this.zone = zone;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.priority = priority;
        this.combinationMode = combinationMode;
        this.scopeProductId = scopeProductId;
        this.scopeCategory = scopeCategory;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public PricingRuleType getType() {
        return type;
    }

    public LocalTime getTimeWindowStart() {
        return timeWindowStart;
    }

    public LocalTime getTimeWindowEnd() {
        return timeWindowEnd;
    }

    public CustomerSegment getSegment() {
        return segment;
    }

    public Integer getMinimumQuantity() {
        return minimumQuantity;
    }

    public String getZone() {
        return zone;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public int getPriority() {
        return priority;
    }

    public CombinationMode getCombinationMode() {
        return combinationMode;
    }

    public UUID getScopeProductId() {
        return scopeProductId;
    }

    public String getScopeCategory() {
        return scopeCategory;
    }

    public PricingRuleStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
