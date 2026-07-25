package com.priceforge.pricing.application.service.support;

import com.priceforge.pricing.application.port.out.PromotionApplierPort;
import com.priceforge.pricing.domain.model.PricingContext;
import com.priceforge.pricing.domain.model.PromotionApplicationOutcome;
import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.ProductId;

/** Stands in for Promotions' real adapter in Pricing's own tests — never applies anything. */
public final class NoOpPromotionApplierPort implements PromotionApplierPort {

    @Override
    public PromotionApplicationOutcome apply(
            Money currentPrice, PricingContext context, ProductId productId, String category) {
        return PromotionApplicationOutcome.none(currentPrice);
    }
}
