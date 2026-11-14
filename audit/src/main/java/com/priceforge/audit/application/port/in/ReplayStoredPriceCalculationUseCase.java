package com.priceforge.audit.application.port.in;

import com.priceforge.audit.application.dto.ReplayStoredPriceCalculationCommand;
import com.priceforge.audit.application.dto.ReplayStoredPriceCalculationResult;

public interface ReplayStoredPriceCalculationUseCase {

    ReplayStoredPriceCalculationResult execute(ReplayStoredPriceCalculationCommand command);
}
