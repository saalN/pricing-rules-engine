package com.priceforge.promotions.infrastructure.in.pricing;

import com.priceforge.pricing.application.port.out.PromotionApplierPort;
import com.priceforge.promotions.application.dto.ApplyPromotionsCommand;
import com.priceforge.promotions.application.port.in.ApplyPromotionsUseCase;
import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.PricingContext;
import com.priceforge.sharedkernel.ProductId;
import com.priceforge.sharedkernel.PromotionApplicationOutcome;
import org.springframework.stereotype.Component;

/**
 * Implements Pricing's outbound extension point (UC-06/UC-11) so Pricing never depends on
 * Promotions: from this context's side, Pricing is a driving caller, so the adapter lives under
 * {@code infrastructure/in} and only translates the call into the {@link ApplyPromotionsUseCase}.
 */
@Component
public class PricingPromotionApplierAdapter implements PromotionApplierPort {

    private final ApplyPromotionsUseCase applyPromotionsUseCase;

    public PricingPromotionApplierAdapter(ApplyPromotionsUseCase applyPromotionsUseCase) {
        this.applyPromotionsUseCase = applyPromotionsUseCase;
    }

    @Override
    public PromotionApplicationOutcome apply(Money currentPrice, PricingContext context, ProductId productId, String category) {
        return applyPromotionsUseCase.execute(new ApplyPromotionsCommand(currentPrice, context, productId, category));
    }
}
