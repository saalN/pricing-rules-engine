package com.priceforge.promotions.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.promotions.application.dto.CreatePromotionCommand;
import com.priceforge.promotions.application.dto.CreatePromotionResult;
import com.priceforge.promotions.application.service.support.FakeCalculateWorstCasePricingDiscountUseCase;
import com.priceforge.promotions.application.service.support.FakeFindCheapestActiveBasePriceUseCase;
import com.priceforge.promotions.application.service.support.InMemoryPromotionRepositoryPort;
import com.priceforge.promotions.application.service.support.RecordingEventPublisher;
import com.priceforge.promotions.domain.exception.InvalidPromotionDiscountException;
import com.priceforge.promotions.domain.model.DiscountType;
import com.priceforge.promotions.domain.model.PromotionCreated;
import com.priceforge.sharedkernel.CombinationMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class CreatePromotionServiceTest {

    private final InMemoryPromotionRepositoryPort repository = new InMemoryPromotionRepositoryPort();
    private final FakeFindCheapestActiveBasePriceUseCase findCheapestActiveBasePrice =
        new FakeFindCheapestActiveBasePriceUseCase();
    private final FakeCalculateWorstCasePricingDiscountUseCase calculateWorstCasePricingDiscount =
        new FakeCalculateWorstCasePricingDiscountUseCase();
    private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
    private final CreatePromotionService service = new CreatePromotionService(
        repository, findCheapestActiveBasePrice, calculateWorstCasePricingDiscount, eventPublisher);

    private CreatePromotionCommand command(DiscountType type, BigDecimal value) {
        return new CreatePromotionCommand(
            "Summer sale", "10% off", Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS),
            1000, 1, null, "URBAN", null, type, value, "EUR", 10, CombinationMode.STACKABLE);
    }

    @Test
    void createsAndPersistsAPromotionAndPublishesPromotionCreated() {
        findCheapestActiveBasePrice.setCheapestBasePriceAmount(BigDecimal.valueOf(100));
        calculateWorstCasePricingDiscount.setWorstCaseDiscountAmount(BigDecimal.ZERO);

        CreatePromotionResult result = service.execute(command(DiscountType.PERCENTAGE, BigDecimal.TEN));

        assertThat(repository.findById(result.id())).isPresent();
        assertThat(eventPublisher.publishedEvents()).anyMatch(PromotionCreated.class::isInstance);
    }

    @Test
    void allowsCreationWhenNoActiveProductExistsInScopeYet() {
        findCheapestActiveBasePrice.setCheapestBasePriceAmount(null);

        CreatePromotionResult result = service.execute(command(DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(50)));

        assertThat(repository.findById(result.id())).isPresent();
    }

    @Test
    void rejectsAPromotionThatCombinedWithWorstCasePricingRulesWouldBreachZero() {
        findCheapestActiveBasePrice.setCheapestBasePriceAmount(BigDecimal.valueOf(100));
        // Worst-case active pricing rules already take 60; a 50 fixed promotion discount on top
        // would drive the 100 base price to -10.
        calculateWorstCasePricingDiscount.setWorstCaseDiscountAmount(BigDecimal.valueOf(60));

        assertThatThrownBy(() -> service.execute(command(DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(50))))
            .isInstanceOf(InvalidPromotionDiscountException.class);
    }

    @Test
    void acceptsAPromotionThatKeepsTheWorstCaseCombinedPricePositive() {
        findCheapestActiveBasePrice.setCheapestBasePriceAmount(BigDecimal.valueOf(100));
        calculateWorstCasePricingDiscount.setWorstCaseDiscountAmount(BigDecimal.valueOf(30));

        CreatePromotionResult result = service.execute(command(DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(20)));

        assertThat(repository.findById(result.id())).isPresent();
    }
}
