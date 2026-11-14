package com.priceforge.promotions.application.dto;

import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.PricingContext;
import com.priceforge.sharedkernel.ProductId;

/** The price already produced by pricing rules, and the context of the calculation it came from. */
public record ApplyPromotionsCommand(Money currentPrice, PricingContext context, ProductId productId, String category) {
}
