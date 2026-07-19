package com.priceforge.pricing.application.port.in;

import com.priceforge.pricing.application.dto.CalculatePriceResult;
import com.priceforge.pricing.application.dto.SimulatePriceCommand;

public interface SimulatePriceUseCase {

    CalculatePriceResult execute(SimulatePriceCommand command);
}
