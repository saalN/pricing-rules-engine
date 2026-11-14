package com.priceforge.promotions.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.sharedkernel.CustomerSegment;
import com.priceforge.promotions.domain.exception.InvalidPromotionStateTransitionException;
import com.priceforge.promotions.domain.exception.InvalidPromotionValidityWindowException;
import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.PromotionId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class PromotionTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final EligibilityCriteria NO_CONSTRAINTS = new EligibilityCriteria(null, null, null);
    private static final Discount TEN_PERCENT_OFF = new Discount(DiscountType.PERCENTAGE, BigDecimal.TEN);
    private static final Money PRICE = Money.of(BigDecimal.valueOf(100), "EUR");

    private static Promotion promotion(Instant validFrom, Instant validUntil, Instant now) {
        return Promotion.create(
            PromotionId.newId(), "Summer sale", "10% off", validFrom, validUntil,
            1000, 1, NO_CONSTRAINTS, TEN_PERCENT_OFF, 10, CombinationMode.STACKABLE, now);
    }

    @Test
    void isActiveWhenTheValidityWindowAlreadyStarted() {
        Promotion promotion = promotion(NOW.minus(1, ChronoUnit.DAYS), NOW.plus(1, ChronoUnit.DAYS), NOW);

        assertThat(promotion.status()).isEqualTo(PromotionStatus.ACTIVE);
    }

    @Test
    void isScheduledWhenTheValidityWindowHasNotStartedYet() {
        Promotion promotion = promotion(NOW.plus(1, ChronoUnit.DAYS), NOW.plus(2, ChronoUnit.DAYS), NOW);

        assertThat(promotion.status()).isEqualTo(PromotionStatus.SCHEDULED);
    }

    @Test
    void rejectsAValidUntilThatDoesNotComeAfterValidFrom() {
        assertThatThrownBy(() -> promotion(NOW, NOW, NOW))
            .isInstanceOf(InvalidPromotionValidityWindowException.class);
        assertThatThrownBy(() -> promotion(NOW.plus(1, ChronoUnit.DAYS), NOW, NOW))
            .isInstanceOf(InvalidPromotionValidityWindowException.class);
    }

    @Test
    void isEligibleWhenActiveWithinWindowAndMatchingCriteria() {
        Promotion promotion = promotion(NOW.minus(1, ChronoUnit.DAYS), NOW.plus(1, ChronoUnit.DAYS), NOW);

        assertThat(promotion.isEligible(CustomerSegment.STANDARD, "URBAN", PRICE, NOW)).isTrue();
    }

    @Test
    void isEligibleWhenScheduledButItsValidFromHasAlreadyPassedInRealTime() {
        // Created while still SCHEDULED (relative to an earlier "now")...
        Promotion promotion = promotion(NOW, NOW.plus(1, ChronoUnit.DAYS), NOW.minus(1, ChronoUnit.HOURS));

        // ...but by the time UC-11 evaluates it, validFrom has already passed — this covers the
        // up-to-a-minute lag before UC-12's scheduler would otherwise flip the persisted status.
        assertThat(promotion.status()).isEqualTo(PromotionStatus.SCHEDULED);
        assertThat(promotion.isEligible(CustomerSegment.STANDARD, "URBAN", PRICE, NOW)).isTrue();
    }

    @Test
    void isNotEligibleOncePastValidUntilEvenIfStillPersistedAsActive() {
        Promotion promotion = promotion(NOW.minus(2, ChronoUnit.DAYS), NOW.minus(1, ChronoUnit.HOURS), NOW.minus(2, ChronoUnit.DAYS));

        assertThat(promotion.status()).isEqualTo(PromotionStatus.ACTIVE);
        assertThat(promotion.isEligible(CustomerSegment.STANDARD, "URBAN", PRICE, NOW)).isFalse();
    }

    @Test
    void isNotEligibleWhenExhaustedOrExpiredRegardlessOfWindow() {
        Promotion exhausted = promotion(NOW.minus(1, ChronoUnit.DAYS), NOW.plus(1, ChronoUnit.DAYS), NOW);
        exhausted.exhaust();
        Promotion expired = promotion(NOW.minus(2, ChronoUnit.DAYS), NOW.minus(1, ChronoUnit.DAYS), NOW.minus(2, ChronoUnit.DAYS));
        expired.expire();

        assertThat(exhausted.isEligible(CustomerSegment.STANDARD, "URBAN", PRICE, NOW)).isFalse();
        assertThat(expired.isEligible(CustomerSegment.STANDARD, "URBAN", PRICE, NOW)).isFalse();
    }

    @Test
    void activatesAScheduledPromotion() {
        Promotion promotion = promotion(NOW.plus(1, ChronoUnit.DAYS), NOW.plus(2, ChronoUnit.DAYS), NOW);

        promotion.activate();

        assertThat(promotion.status()).isEqualTo(PromotionStatus.ACTIVE);
    }

    @Test
    void rejectsActivatingAPromotionThatIsNotScheduled() {
        Promotion promotion = promotion(NOW.minus(1, ChronoUnit.DAYS), NOW.plus(1, ChronoUnit.DAYS), NOW);

        assertThatThrownBy(promotion::activate).isInstanceOf(InvalidPromotionStateTransitionException.class);
    }

    @Test
    void expiresAnActivePromotion() {
        Promotion promotion = promotion(NOW.minus(1, ChronoUnit.DAYS), NOW.plus(1, ChronoUnit.DAYS), NOW);

        promotion.expire();

        assertThat(promotion.status()).isEqualTo(PromotionStatus.EXPIRED);
    }

    @Test
    void rejectsExpiringAPromotionThatIsNotActive() {
        Promotion promotion = promotion(NOW.plus(1, ChronoUnit.DAYS), NOW.plus(2, ChronoUnit.DAYS), NOW);

        assertThatThrownBy(promotion::expire).isInstanceOf(InvalidPromotionStateTransitionException.class);
    }

    @Test
    void exhaustsAnActivePromotion() {
        Promotion promotion = promotion(NOW.minus(1, ChronoUnit.DAYS), NOW.plus(1, ChronoUnit.DAYS), NOW);

        promotion.exhaust();

        assertThat(promotion.status()).isEqualTo(PromotionStatus.EXHAUSTED);
    }

    @Test
    void rejectsExhaustingAPromotionThatIsNotActive() {
        Promotion promotion = promotion(NOW.plus(1, ChronoUnit.DAYS), NOW.plus(2, ChronoUnit.DAYS), NOW);

        assertThatThrownBy(promotion::exhaust).isInstanceOf(InvalidPromotionStateTransitionException.class);
    }

    @Test
    void exposesConflictResolvableFields() {
        Promotion promotion = promotion(NOW.minus(1, ChronoUnit.DAYS), NOW.plus(1, ChronoUnit.DAYS), NOW);

        assertThat(promotion.conflictId()).isEqualTo(promotion.id());
        assertThat(promotion.combinationMode()).isEqualTo(CombinationMode.STACKABLE);
        assertThat(promotion.priority()).isEqualTo(10);
        assertThat(promotion.createdAt()).isEqualTo(NOW);
    }
}
