package com.priceforge.catalog.infrastructure.in.http.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record UpdateProductBasePriceRequest(
    @NotNull @Positive BigDecimal newBasePriceAmount
) {
}
