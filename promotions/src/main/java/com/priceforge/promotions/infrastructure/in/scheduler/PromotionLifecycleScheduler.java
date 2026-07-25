package com.priceforge.promotions.infrastructure.in.scheduler;

import com.priceforge.promotions.application.port.in.ActivateAndExpirePromotionsUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * UC-12: runs once a minute (the granularity the functional spec itself calls out as the reason
 * UC-11 also evaluates the validity window in real time, to bound the lag this cadence allows).
 */
@Component
public class PromotionLifecycleScheduler {

    private final ActivateAndExpirePromotionsUseCase activateAndExpirePromotionsUseCase;

    public PromotionLifecycleScheduler(ActivateAndExpirePromotionsUseCase activateAndExpirePromotionsUseCase) {
        this.activateAndExpirePromotionsUseCase = activateAndExpirePromotionsUseCase;
    }

    @Scheduled(fixedDelayString = "${priceforge.promotions.lifecycle-scheduler-interval-ms:60000}")
    public void activateAndExpirePromotions() {
        activateAndExpirePromotionsUseCase.execute();
    }
}
