package com.priceforge.pricing.application.port.in;

import com.priceforge.pricing.application.dto.ReplayPriceCalculationCommand;
import com.priceforge.pricing.application.dto.ReplayPriceCalculationResult;

/**
 * Cross-context query consumed by Audit (UC-14) to re-run a stored calculation against the
 * currently active rules, without exposing Pricing's rule/context domain internals.
 */
public interface ReplayPriceCalculationUseCase {

    ReplayPriceCalculationResult execute(ReplayPriceCalculationCommand command);
}
