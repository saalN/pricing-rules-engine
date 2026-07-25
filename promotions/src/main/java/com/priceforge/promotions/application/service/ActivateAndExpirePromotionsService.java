package com.priceforge.promotions.application.service;

import com.priceforge.promotions.application.dto.ActivateAndExpirePromotionsResult;
import com.priceforge.promotions.application.port.in.ActivateAndExpirePromotionsUseCase;
import com.priceforge.promotions.application.port.out.PromotionRepositoryPort;
import com.priceforge.promotions.domain.model.Promotion;
import com.priceforge.promotions.domain.model.PromotionActivated;
import com.priceforge.promotions.domain.model.PromotionExpired;
import com.priceforge.sharedkernel.EventPublisher;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-12: SCHEDULED -> ACTIVE once validFrom is reached, ACTIVE -> EXPIRED once validUntil has
 * passed. EXHAUSTED promotions are never considered here since {@code findAllActive()} only
 * returns promotions whose status is ACTIVE — that transition is usage-driven (UC-11), not
 * time-driven.
 */
@Service
public class ActivateAndExpirePromotionsService implements ActivateAndExpirePromotionsUseCase {

    private final PromotionRepositoryPort promotionRepository;
    private final EventPublisher eventPublisher;

    public ActivateAndExpirePromotionsService(
            PromotionRepositoryPort promotionRepository,
            @Qualifier("promotionsEventPublisher") EventPublisher eventPublisher) {
        this.promotionRepository = promotionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ActivateAndExpirePromotionsResult execute() {
        Instant now = Instant.now();

        int activatedCount = 0;
        for (Promotion promotion : promotionRepository.findAllScheduled()) {
            if (!now.isBefore(promotion.validFrom())) {
                promotion.activate();
                promotionRepository.save(promotion);
                eventPublisher.publish(PromotionActivated.of(promotion));
                activatedCount++;
            }
        }

        int expiredCount = 0;
        for (Promotion promotion : promotionRepository.findAllActive()) {
            if (!now.isBefore(promotion.validUntil())) {
                promotion.expire();
                promotionRepository.save(promotion);
                eventPublisher.publish(PromotionExpired.of(promotion));
                expiredCount++;
            }
        }

        return new ActivateAndExpirePromotionsResult(activatedCount, expiredCount);
    }
}
