package com.priceforge.promotions.application.dto;

import com.priceforge.pricing.domain.model.CustomerSegment;
import com.priceforge.promotions.domain.model.DiscountType;
import com.priceforge.sharedkernel.CombinationMode;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * {@code segment}, {@code category} and {@code minimumPurchaseAmount} are each optional
 * eligibility constraints (UC-10). {@code currency} denominates {@code discountValue} when
 * {@code discountType} is {@code FIXED_AMOUNT} and anchors UC-13's zero-price validation — the
 * platform never converts currencies, so this must be the currency of the products this
 * promotion targets.
 */
public record CreatePromotionCommand(
    String name,
    String description,
    Instant validFrom,
    Instant validUntil,
    int globalUsageLimit,
    int perUserUsageLimit,
    CustomerSegment segment,
    String category,
    BigDecimal minimumPurchaseAmount,
    DiscountType discountType,
    BigDecimal discountValue,
    String currency,
    int priority,
    CombinationMode combinationMode
) {
}
