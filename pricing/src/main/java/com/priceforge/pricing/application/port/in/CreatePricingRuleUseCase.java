package com.priceforge.pricing.application.port.in;

import com.priceforge.pricing.application.dto.CreatePricingRuleCommand;
import com.priceforge.pricing.application.dto.CreatePricingRuleResult;

public interface CreatePricingRuleUseCase {

    CreatePricingRuleResult execute(CreatePricingRuleCommand command);
}
