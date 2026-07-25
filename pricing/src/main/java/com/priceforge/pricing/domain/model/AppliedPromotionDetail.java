package com.priceforge.pricing.domain.model;

import com.priceforge.sharedkernel.Money;

/**
 * Pricing's own generic view of "an external discount source applied on top of the pricing-rules
 * result" — deliberately not typed to Promotions' own vocabulary, since Pricing must never depend
 * on Promotions (see {@code PromotionApplierPort}).
 */
public record AppliedPromotionDetail(String sourceId, String description, Money discountApplied, String reason) {
}
