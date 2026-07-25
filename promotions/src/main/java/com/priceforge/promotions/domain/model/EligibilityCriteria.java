package com.priceforge.promotions.domain.model;

import com.priceforge.pricing.domain.model.CustomerSegment;
import com.priceforge.sharedkernel.Money;
import java.math.BigDecimal;

/**
 * UC-10's eligibility conditions: segment, category and minimum purchase are each optional
 * (a {@code null} field means "no constraint on that dimension"), evaluated together as a
 * conjunction — unlike Pricing's {@code RuleCondition}, which is a discriminated union of exactly
 * one condition kind. {@code CustomerSegment} is reused directly from Pricing rather than
 * duplicated: {@code PromotionApplierPort} already forces this adapter to consume Pricing's
 * {@code PricingContext} (which embeds this same type), so there is no boundary left to protect
 * by introducing a second, always-mirrored enum.
 */
public record EligibilityCriteria(CustomerSegment segment, String category, BigDecimal minimumPurchaseAmount) {

    public boolean matches(CustomerSegment contextSegment, String contextCategory, Money currentPrice) {
        if (segment != null && segment != contextSegment) {
            return false;
        }
        if (category != null && !category.equalsIgnoreCase(contextCategory)) {
            return false;
        }
        if (minimumPurchaseAmount != null && currentPrice.amount().compareTo(minimumPurchaseAmount) < 0) {
            return false;
        }
        return true;
    }
}
