package com.priceforge.promotions.application.service;

import com.priceforge.sharedkernel.AppliedPromotionDetail;
import java.util.List;
import java.util.ArrayList;
import static org.assertj.core.api.Assertions.assertThat;

import com.priceforge.sharedkernel.CustomerSegment;
import com.priceforge.sharedkernel.ExcludedPromotionDetail;
import com.priceforge.sharedkernel.PricingContext;
import com.priceforge.sharedkernel.PromotionApplicationOutcome;
import com.priceforge.promotions.application.service.support.InMemoryPromotionRepositoryPort;
import com.priceforge.promotions.application.service.support.RecordingEventPublisher;
import com.priceforge.promotions.application.dto.ApplyPromotionsCommand;
import com.priceforge.promotions.domain.model.Discount;
import com.priceforge.promotions.domain.model.DiscountType;
import com.priceforge.promotions.domain.model.EligibilityCriteria;
import com.priceforge.promotions.domain.model.Promotion;
import com.priceforge.promotions.domain.model.PromotionApplied;
import com.priceforge.promotions.domain.model.PromotionExhausted;
import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.ProductId;
import com.priceforge.sharedkernel.PromotionId;
import com.priceforge.sharedkernel.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class ApplyPromotionsServiceTest {

    private final InMemoryPromotionRepositoryPort repository = new InMemoryPromotionRepositoryPort();
    private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
    private final ApplyPromotionsService service = new ApplyPromotionsService(repository, eventPublisher);

    private static final EligibilityCriteria NO_CONSTRAINTS = new EligibilityCriteria(null, null, null);
    private final ProductId productId = ProductId.newId();
    private final UserId userId = UserId.newId();

    private Promotion activePromotion(
            String name, EligibilityCriteria eligibility, Discount discount, int priority,
            CombinationMode combinationMode, int globalLimit, int perUserLimit) {
        Instant now = Instant.now();
        Promotion promotion = Promotion.create(
            PromotionId.newId(), name, "d", now.minus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS),
            globalLimit, perUserLimit, eligibility, discount, priority, combinationMode, now);
        repository.save(promotion);
        return promotion;
    }

    private PricingContext context() {
        return new PricingContext(
            productId, userId, CustomerSegment.STANDARD, 1, Instant.now(), "URBAN", "EUR", ZoneId.of("UTC"));
    }

    @Test
    void appliesASingleEligiblePromotionAndPublishesPromotionApplied() {
        activePromotion(
            "10% off", NO_CONSTRAINTS, new Discount(DiscountType.PERCENTAGE, BigDecimal.TEN), 10,
            CombinationMode.STACKABLE, 1000, 10);

        PromotionApplicationOutcome outcome = service.execute(new ApplyPromotionsCommand(
            Money.of(BigDecimal.valueOf(100), "EUR"), context(), productId, "URBAN"));

        assertThat(outcome.newPrice().amount()).isEqualByComparingTo(BigDecimal.valueOf(90));
        assertThat(outcome.applied()).hasSize(1);
        assertThat(eventPublisher.publishedEvents()).anyMatch(PromotionApplied.class::isInstance);
    }

    @Test
    void skipsPromotionsThatAreNotEligible() {
        activePromotion(
            "VIP only", new EligibilityCriteria(CustomerSegment.VIP, null, null),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.TEN), 10, CombinationMode.STACKABLE, 1000, 10);

        PromotionApplicationOutcome outcome = service.execute(new ApplyPromotionsCommand(
            Money.of(BigDecimal.valueOf(100), "EUR"), context(), productId, "URBAN"));

        assertThat(outcome.applied()).isEmpty();
        assertThat(outcome.excluded()).isEmpty();
        assertThat(outcome.newPrice().amount()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void excludesALowerPriorityStackablePromotionWhenAHigherPriorityExclusiveApplies() {
        activePromotion(
            "Exclusive winner", NO_CONSTRAINTS, new Discount(DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(20)),
            20, CombinationMode.EXCLUSIVE, 1000, 10);
        activePromotion(
            "Stackable loser", NO_CONSTRAINTS, new Discount(DiscountType.FIXED_AMOUNT, BigDecimal.TEN),
            5, CombinationMode.STACKABLE, 1000, 10);

        PromotionApplicationOutcome outcome = service.execute(new ApplyPromotionsCommand(
            Money.of(BigDecimal.valueOf(100), "EUR"), context(), productId, "URBAN"));

        assertThat(outcome.newPrice().amount()).isEqualByComparingTo(BigDecimal.valueOf(80));
        assertThat(outcome.applied()).hasSize(1);
        assertThat(outcome.excluded()).hasSize(1);
        assertThat(outcome.excluded().get(0).reason()).isEqualTo("EXCLUDED_BY_HIGHER_PRIORITY");
    }

    @Test
    void doesNotApplyAPromotionToAUserWhoHasAlreadyReachedTheirPerUserLimit() {
        // Global limit is far from reached, so the promotion is still ACTIVE — only this specific
        // user has exhausted their own independent per-user limit.
        Promotion promotion = activePromotion(
            "One per customer", NO_CONSTRAINTS, new Discount(DiscountType.FIXED_AMOUNT, BigDecimal.TEN), 10,
            CombinationMode.STACKABLE, 1000, 1);
        repository.recordUsageIfWithinLimits(promotion.id(), userId);

        PromotionApplicationOutcome outcome = service.execute(new ApplyPromotionsCommand(
            Money.of(BigDecimal.valueOf(100), "EUR"), context(), productId, "URBAN"));

        assertThat(outcome.applied()).isEmpty();
        assertThat(outcome.excluded()).hasSize(1);
        ExcludedPromotionDetail excluded = outcome.excluded().get(0);
        assertThat(excluded.reason()).isEqualTo("LIMIT_REACHED");
        assertThat(outcome.newPrice().amount()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void transitionsToExhaustedAndPublishesPromotionExhaustedWhenTheGlobalLimitIsReachedByThisCall() {
        Promotion promotion = activePromotion(
            "Last slot", NO_CONSTRAINTS, new Discount(DiscountType.FIXED_AMOUNT, BigDecimal.TEN), 10,
            CombinationMode.STACKABLE, 1, 10);

        PromotionApplicationOutcome outcome = service.execute(new ApplyPromotionsCommand(
            Money.of(BigDecimal.valueOf(100), "EUR"), context(), productId, "URBAN"));

        assertThat(outcome.applied()).hasSize(1);
        assertThat(eventPublisher.publishedEvents()).anyMatch(PromotionExhausted.class::isInstance);
        assertThat(repository.findById(promotion.id()).orElseThrow().status().name()).isEqualTo("EXHAUSTED");
    }

    @Test
    void floorsThePriceAtTheMinimumPositiveAmountRatherThanGoingToZeroOrNegative() {
        activePromotion(
            "Huge discount", NO_CONSTRAINTS, new Discount(DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(150)), 10,
            CombinationMode.STACKABLE, 1000, 10);

        PromotionApplicationOutcome outcome = service.execute(new ApplyPromotionsCommand(
            Money.of(BigDecimal.valueOf(100), "EUR"), context(), productId, "URBAN"));

        assertThat(outcome.priceFloorApplied()).isTrue();
        assertThat(outcome.newPrice().amount()).isEqualByComparingTo(BigDecimal.valueOf(0.01));
    }

    @Test
    void sumsMultipleStackablePromotionsAgainstTheSameStartingPrice() {
        activePromotion(
            "5 off", NO_CONSTRAINTS, new Discount(DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(5)), 10,
            CombinationMode.STACKABLE, 1000, 10);
        activePromotion(
            "10 off", NO_CONSTRAINTS, new Discount(DiscountType.FIXED_AMOUNT, BigDecimal.TEN), 10,
            CombinationMode.STACKABLE, 1000, 10);

        PromotionApplicationOutcome outcome = service.execute(new ApplyPromotionsCommand(
            Money.of(BigDecimal.valueOf(100), "EUR"), context(), productId, "URBAN"));

        assertThat(outcome.newPrice().amount()).isEqualByComparingTo(BigDecimal.valueOf(85));
        assertThat(outcome.applied()).hasSize(2);
    }

    @Test
    void promotionsTiedAtOnePriorityAreAlwaysProcessedInIdOrder() {
        // Concurrent requests lock tied promotions one after another; a fixed order is what keeps two
        // requests from locking the same pair in opposite orders.
        List<Promotion> tied = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            tied.add(activePromotion(
                "Tied " + i, NO_CONSTRAINTS, new Discount(DiscountType.FIXED_AMOUNT, BigDecimal.ONE), 10,
                CombinationMode.STACKABLE, 1000, 10));
        }

        PromotionApplicationOutcome outcome = service.execute(new ApplyPromotionsCommand(
            Money.of(BigDecimal.valueOf(100), "EUR"), context(), productId, "URBAN"));

        List<String> expected = tied.stream().map(promotion -> promotion.id().value()).sorted()
            .map(Object::toString).toList();
        assertThat(outcome.applied()).extracting(AppliedPromotionDetail::sourceId).containsExactlyElementsOf(expected);
    }
}
