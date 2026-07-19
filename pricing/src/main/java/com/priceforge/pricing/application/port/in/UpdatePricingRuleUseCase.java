package com.priceforge.pricing.application.port.in;

import com.priceforge.pricing.application.dto.UpdatePricingRuleCommand;
import com.priceforge.pricing.application.dto.UpdatePricingRuleResult;

public interface UpdatePricingRuleUseCase {

    UpdatePricingRuleResult execute(UpdatePricingRuleCommand command);
}
