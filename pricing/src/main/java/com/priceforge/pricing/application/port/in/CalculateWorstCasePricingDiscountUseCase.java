package com.priceforge.pricing.application.port.in;

import com.priceforge.pricing.application.dto.CalculateWorstCasePricingDiscountQuery;
import com.priceforge.pricing.application.dto.CalculateWorstCasePricingDiscountResult;

/**
 * Cross-context query consumed by Promotions (UC-13) to build its conservative-upper-bound
 * validation scenario: the largest discount active Pricing rules could apply to a given base
 * price in a given scope, without exposing any of Pricing's rule/scope/discount domain internals
 * across the boundary.
 */
public interface CalculateWorstCasePricingDiscountUseCase {

    CalculateWorstCasePricingDiscountResult execute(CalculateWorstCasePricingDiscountQuery query);
}
