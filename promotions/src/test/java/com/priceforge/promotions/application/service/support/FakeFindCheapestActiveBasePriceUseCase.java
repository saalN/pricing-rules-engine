package com.priceforge.promotions.application.service.support;

import com.priceforge.catalog.application.dto.FindCheapestActiveBasePriceQuery;
import com.priceforge.catalog.application.dto.FindCheapestActiveBasePriceResult;
import com.priceforge.catalog.application.port.in.FindCheapestActiveBasePriceUseCase;
import java.math.BigDecimal;

/** In-memory stand-in for Catalog's cross-context query, for Promotions' application-layer tests. */
public final class FakeFindCheapestActiveBasePriceUseCase implements FindCheapestActiveBasePriceUseCase {

    private BigDecimal cheapestBasePriceAmount;

    public void setCheapestBasePriceAmount(BigDecimal amount) {
        this.cheapestBasePriceAmount = amount;
    }

    @Override
    public FindCheapestActiveBasePriceResult execute(FindCheapestActiveBasePriceQuery query) {
        return new FindCheapestActiveBasePriceResult(cheapestBasePriceAmount);
    }
}
