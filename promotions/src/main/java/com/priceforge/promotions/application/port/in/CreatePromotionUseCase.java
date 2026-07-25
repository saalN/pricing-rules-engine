package com.priceforge.promotions.application.port.in;

import com.priceforge.promotions.application.dto.CreatePromotionCommand;
import com.priceforge.promotions.application.dto.CreatePromotionResult;

public interface CreatePromotionUseCase {

    CreatePromotionResult execute(CreatePromotionCommand command);
}
