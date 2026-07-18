package com.priceforge.catalog.application.port.in;

import com.priceforge.catalog.application.dto.ChangeProductCategoryCommand;
import com.priceforge.catalog.application.dto.ChangeProductCategoryResult;

public interface ChangeProductCategoryUseCase {

    ChangeProductCategoryResult execute(ChangeProductCategoryCommand command);
}
