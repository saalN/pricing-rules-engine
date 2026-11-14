package com.priceforge.promotions.infrastructure.out.postgres.entity;

import com.priceforge.sharedkernel.CustomerSegment;
import com.priceforge.promotions.domain.model.DiscountType;
import com.priceforge.promotions.domain.model.PromotionStatus;
import com.priceforge.sharedkernel.CombinationMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** {@code globalUsageCount} is updated only through the adapter's atomic JDBC operation, never via {@code save()}. */
@Entity
@Table(name = "promotion", schema = "promotions")
public class PromotionDocument {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "global_usage_limit", nullable = false)
    private int globalUsageLimit;

    @Column(name = "per_user_usage_limit", nullable = false)
    private int perUserUsageLimit;

    @Column(name = "global_usage_count", nullable = false)
    private int globalUsageCount;

    @Enumerated(EnumType.STRING)
    private CustomerSegment segment;

    private String category;

    @Column(name = "minimum_purchase_amount")
    private BigDecimal minimumPurchaseAmount;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PromotionDocument() {
        // required by JPA
    }

    public PromotionDocument(
            UUID id,
            String name,
            String description,
            Instant validFrom,
            Instant validUntil,
            int globalUsageLimit,
            int perUserUsageLimit,
            int globalUsageCount,
            CustomerSegment segment,
            String category,
            BigDecimal minimumPurchaseAmount,
            DiscountType discountType,
            BigDecimal discountValue,
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
        this.globalUsageCount = globalUsageCount;
        this.segment = segment;
        this.category = category;
        this.minimumPurchaseAmount = minimumPurchaseAmount;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.priority = priority;
        this.combinationMode = combinationMode;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public int getGlobalUsageLimit() {
        return globalUsageLimit;
    }

    public int getPerUserUsageLimit() {
        return perUserUsageLimit;
    }

    public int getGlobalUsageCount() {
        return globalUsageCount;
    }

    public CustomerSegment getSegment() {
        return segment;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getMinimumPurchaseAmount() {
        return minimumPurchaseAmount;
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

    public PromotionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
