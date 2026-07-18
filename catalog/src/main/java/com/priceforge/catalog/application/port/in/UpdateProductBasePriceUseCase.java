package com.priceforge.catalog.application.port.in;

import com.priceforge.catalog.application.dto.UpdateProductBasePriceCommand;
import com.priceforge.catalog.application.dto.UpdateProductBasePriceResult;

public interface UpdateProductBasePriceUseCase {

    UpdateProductBasePriceResult execute(UpdateProductBasePriceCommand command);
}
