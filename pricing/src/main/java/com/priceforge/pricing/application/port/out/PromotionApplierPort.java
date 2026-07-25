package com.priceforge.pricing.application.port.out;

import com.priceforge.pricing.domain.model.PricingContext;
import com.priceforge.pricing.domain.model.PromotionApplicationOutcome;
import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.ProductId;

/**
 * UC-06/UC-11's extension point: Promotions applies on top of the price already resulting from
 * pricing rules. Pricing defines this port using only its own vocabulary (arquitectura-tecnica.md
 * calls it "the conflict-resolution extension point") so Promotions can implement it without
 * Pricing ever depending on Promotions — the dependency points the other way (Promotions depends
 * on Pricing's ports), so Pricing cannot call into Promotions directly.
 */
public interface PromotionApplierPort {

    PromotionApplicationOutcome apply(Money currentPrice, PricingContext context, ProductId productId, String category);
}
