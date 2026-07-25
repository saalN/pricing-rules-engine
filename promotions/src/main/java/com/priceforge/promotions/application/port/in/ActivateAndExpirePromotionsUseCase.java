package com.priceforge.promotions.application.port.in;

import com.priceforge.promotions.application.dto.ActivateAndExpirePromotionsResult;

/** UC-12: driven by a scheduled job (once a minute) in app's infrastructure, not by a user. */
public interface ActivateAndExpirePromotionsUseCase {

    ActivateAndExpirePromotionsResult execute();
}
