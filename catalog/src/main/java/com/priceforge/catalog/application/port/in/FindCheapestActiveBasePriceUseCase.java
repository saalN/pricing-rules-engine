package com.priceforge.catalog.application.port.in;

import com.priceforge.catalog.application.dto.FindCheapestActiveBasePriceQuery;
import com.priceforge.catalog.application.dto.FindCheapestActiveBasePriceResult;

/**
 * Cross-context query consumed by Pricing (and later Promotions) to validate that a discount can
 * never drive an in-scope product's price to zero or negative — the same "cheapest product in
 * scope" heuristic the functional spec uses for UC-13's promotion validation.
 */
public interface FindCheapestActiveBasePriceUseCase {

    FindCheapestActiveBasePriceResult execute(FindCheapestActiveBasePriceQuery query);
}
