package com.priceforge.promotions.infrastructure.in.http.dto;

import com.priceforge.sharedkernel.CustomerSegment;
import com.priceforge.promotions.domain.model.DiscountType;
import com.priceforge.sharedkernel.CombinationMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * UC-10's campaign definition. {@code segment}, {@code category} and
 * {@code minimumPurchaseAmount} are each optional eligibility constraints — all absent means the
 * promotion applies to every eligible request.
 */
public record CreatePromotionRequest(
    @NotBlank String name,
    String description,
    @NotNull Instant validFrom,
    @NotNull Instant validUntil,
    @Positive int globalUsageLimit,
    @Positive int perUserUsageLimit,
    CustomerSegment segment,
    String category,
    BigDecimal minimumPurchaseAmount,
    @NotNull DiscountType discountType,
    @NotNull @Positive BigDecimal discountValue,
    @NotBlank String currency,
    int priority,
    @NotNull CombinationMode combinationMode
) {
}
