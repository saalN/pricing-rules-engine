package com.priceforge.pricing.application.port.out;

import com.priceforge.pricing.domain.model.PricingRule;
import com.priceforge.sharedkernel.PricingRuleId;
import java.util.List;
import java.util.Optional;

public interface PricingRuleRepositoryPort {

    PricingRule save(PricingRule rule);

    Optional<PricingRule> findById(PricingRuleId id);

    /**
     * Every ACTIVE rule, unfiltered by scope or condition — {@code PriceCalculatorService}
     * applies both filters itself, so this stays a simple, cacheable-if-needed query.
     */
    List<PricingRule> findAllActive();
}
