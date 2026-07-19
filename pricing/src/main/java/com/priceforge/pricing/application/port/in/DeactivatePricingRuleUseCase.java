package com.priceforge.pricing.application.port.in;

import com.priceforge.pricing.application.dto.DeactivatePricingRuleCommand;
import com.priceforge.pricing.application.dto.DeactivatePricingRuleResult;

public interface DeactivatePricingRuleUseCase {

    DeactivatePricingRuleResult execute(DeactivatePricingRuleCommand command);
}
