package com.priceforge.catalog.application.port.in;

import com.priceforge.catalog.application.dto.DeactivateProductCommand;
import com.priceforge.catalog.application.dto.DeactivateProductResult;

public interface DeactivateProductUseCase {

    DeactivateProductResult execute(DeactivateProductCommand command);
}
