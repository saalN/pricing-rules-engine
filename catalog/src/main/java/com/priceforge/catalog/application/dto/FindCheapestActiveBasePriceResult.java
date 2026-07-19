package com.priceforge.catalog.application.dto;

import java.math.BigDecimal;

/**
 * {@code cheapestBasePriceAmount} is null when no active product matches the requested scope.
 */
public record FindCheapestActiveBasePriceResult(BigDecimal cheapestBasePriceAmount) {
}
