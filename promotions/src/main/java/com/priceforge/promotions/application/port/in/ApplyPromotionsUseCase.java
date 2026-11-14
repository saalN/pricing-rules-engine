package com.priceforge.promotions.application.port.in;

import com.priceforge.promotions.application.dto.ApplyPromotionsCommand;
import com.priceforge.sharedkernel.PromotionApplicationOutcome;

/** UC-11: applies every eligible promotion on top of a price, consuming usage limits atomically. */
public interface ApplyPromotionsUseCase {

    PromotionApplicationOutcome execute(ApplyPromotionsCommand command);
}
