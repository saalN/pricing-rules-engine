package com.priceforge.catalog.application.port.in;

import com.priceforge.catalog.application.dto.CreateProductCommand;
import com.priceforge.catalog.application.dto.CreateProductResult;

public interface CreateProductUseCase {

    CreateProductResult execute(CreateProductCommand command);
}
