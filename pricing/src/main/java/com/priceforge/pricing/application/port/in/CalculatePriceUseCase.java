package com.priceforge.pricing.application.port.in;

import com.priceforge.pricing.application.dto.CalculatePriceCommand;
import com.priceforge.pricing.application.dto.CalculatePriceResult;

public interface CalculatePriceUseCase {

    CalculatePriceResult execute(CalculatePriceCommand command);
}
