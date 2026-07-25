package com.priceforge.promotions.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.priceforge.promotions.application.dto.ActivateAndExpirePromotionsResult;
import com.priceforge.promotions.application.service.support.InMemoryPromotionRepositoryPort;
import com.priceforge.promotions.application.service.support.RecordingEventPublisher;
import com.priceforge.promotions.domain.model.Discount;
import com.priceforge.promotions.domain.model.DiscountType;
import com.priceforge.promotions.domain.model.EligibilityCriteria;
import com.priceforge.promotions.domain.model.Promotion;
import com.priceforge.promotions.domain.model.PromotionActivated;
import com.priceforge.promotions.domain.model.PromotionExpired;
import com.priceforge.promotions.domain.model.PromotionStatus;
import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.sharedkernel.PromotionId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class ActivateAndExpirePromotionsServiceTest {

    private final InMemoryPromotionRepositoryPort repository = new InMemoryPromotionRepositoryPort();
    private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
    private final ActivateAndExpirePromotionsService service =
        new ActivateAndExpirePromotionsService(repository, eventPublisher);

    private static final EligibilityCriteria NO_CONSTRAINTS = new EligibilityCriteria(null, null, null);
    private static final Discount DISCOUNT = new Discount(DiscountType.PERCENTAGE, BigDecimal.TEN);

    @Test
    void activatesAScheduledPromotionWhoseValidFromHasArrived() {
        Instant createdAt = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant validFrom = Instant.now().minus(1, ChronoUnit.HOURS);
        Promotion promotion = Promotion.create(
            PromotionId.newId(), "Flash sale", "d", validFrom, Instant.now().plus(1, ChronoUnit.DAYS),
            1000, 1, NO_CONSTRAINTS, DISCOUNT, 10, CombinationMode.STACKABLE, createdAt);
        repository.save(promotion);

        ActivateAndExpirePromotionsResult result = service.execute();

        assertThat(result.activatedCount()).isEqualTo(1);
        assertThat(repository.findById(promotion.id()).orElseThrow().status()).isEqualTo(PromotionStatus.ACTIVE);
        assertThat(eventPublisher.publishedEvents()).anyMatch(PromotionActivated.class::isInstance);
    }

    @Test
    void doesNotActivateAScheduledPromotionWhoseValidFromHasNotArrivedYet() {
        Promotion promotion = Promotion.create(
            PromotionId.newId(), "Future sale", "d", Instant.now().plus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), 1000, 1, NO_CONSTRAINTS, DISCOUNT, 10,
            CombinationMode.STACKABLE, Instant.now());
        repository.save(promotion);

        ActivateAndExpirePromotionsResult result = service.execute();

        assertThat(result.activatedCount()).isZero();
        assertThat(repository.findById(promotion.id()).orElseThrow().status()).isEqualTo(PromotionStatus.SCHEDULED);
    }

    @Test
    void expiresAnActivePromotionPastItsValidUntil() {
        Instant createdAt = Instant.now().minus(3, ChronoUnit.DAYS);
        Promotion promotion = Promotion.create(
            PromotionId.newId(), "Past sale", "d", createdAt, Instant.now().minus(1, ChronoUnit.HOURS),
            1000, 1, NO_CONSTRAINTS, DISCOUNT, 10, CombinationMode.STACKABLE, createdAt);
        repository.save(promotion);

        ActivateAndExpirePromotionsResult result = service.execute();

        assertThat(result.expiredCount()).isEqualTo(1);
        assertThat(repository.findById(promotion.id()).orElseThrow().status()).isEqualTo(PromotionStatus.EXPIRED);
        assertThat(eventPublisher.publishedEvents()).anyMatch(PromotionExpired.class::isInstance);
    }

    @Test
    void neverExpiresAnAlreadyExhaustedPromotion() {
        Instant createdAt = Instant.now().minus(3, ChronoUnit.DAYS);
        Promotion promotion = Promotion.create(
            PromotionId.newId(), "Exhausted sale", "d", createdAt, Instant.now().minus(1, ChronoUnit.HOURS),
            1000, 1, NO_CONSTRAINTS, DISCOUNT, 10, CombinationMode.STACKABLE, createdAt);
        promotion.exhaust();
        repository.save(promotion);

        ActivateAndExpirePromotionsResult result = service.execute();

        assertThat(result.expiredCount()).isZero();
        assertThat(repository.findById(promotion.id()).orElseThrow().status()).isEqualTo(PromotionStatus.EXHAUSTED);
    }
}
