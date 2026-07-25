package com.priceforge.pricing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.catalog.application.dto.GetProductResult;
import com.priceforge.pricing.application.dto.CreatePricingRuleCommand;
import com.priceforge.pricing.application.dto.CreatePricingRuleResult;
import com.priceforge.pricing.application.service.support.FakeFindCheapestActiveBasePriceUseCase;
import com.priceforge.pricing.application.service.support.FakeGetProductUseCase;
import com.priceforge.pricing.application.service.support.InMemoryPricingRuleRepositoryPort;
import com.priceforge.pricing.application.service.support.RecordingEventPublisher;
import com.priceforge.pricing.domain.exception.InvalidDiscountException;
import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.PricingRuleCreated;
import com.priceforge.pricing.domain.model.PricingRuleType;
import com.priceforge.sharedkernel.ProductId;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class CreatePricingRuleServiceTest {

    private final InMemoryPricingRuleRepositoryPort pricingRuleRepository = new InMemoryPricingRuleRepositoryPort();
    private final FakeGetProductUseCase getProductUseCase = new FakeGetProductUseCase();
    private final FakeFindCheapestActiveBasePriceUseCase findCheapestActiveBasePriceUseCase =
        new FakeFindCheapestActiveBasePriceUseCase();
    private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
    private final CreatePricingRuleService service = new CreatePricingRuleService(
        pricingRuleRepository, getProductUseCase, findCheapestActiveBasePriceUseCase, eventPublisher);

    private CreatePricingRuleCommand timeBasedCommand(DiscountType discountType, BigDecimal discountValue) {
        return new CreatePricingRuleCommand(
            PricingRuleType.TIME_BASED, LocalTime.of(22, 0), LocalTime.of(6, 0),
            null, null, null, discountType, discountValue, 10, CombinationMode.STACKABLE, null, null);
    }

    @Test
    void createsARuleAndPublishesPricingRuleCreated() {
        CreatePricingRuleResult result = service.execute(
            timeBasedCommand(DiscountType.PERCENTAGE, BigDecimal.valueOf(20)));

        assertThat(result.ruleId()).isNotNull();
        assertThat(pricingRuleRepository.findById(result.ruleId())).isPresent();
        assertThat(eventPublisher.publishedEvents()).hasSize(1);
        assertThat(eventPublisher.publishedEvents().get(0)).isInstanceOf(PricingRuleCreated.class);
    }

    @Test
    void rejectsAPercentageDiscountOf100OrMore() {
        assertThatThrownBy(() -> service.execute(timeBasedCommand(DiscountType.PERCENTAGE, BigDecimal.valueOf(100))))
            .isInstanceOf(InvalidDiscountException.class);
        assertThat(eventPublisher.publishedEvents()).isEmpty();
    }

    @Test
    void rejectsAFixedAmountThatWouldBreachTheCheapestActiveProductInScope() {
        findCheapestActiveBasePriceUseCase.setCheapestBasePriceAmount(BigDecimal.valueOf(10));

        assertThatThrownBy(() -> service.execute(timeBasedCommand(DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(10))))
            .isInstanceOf(InvalidDiscountException.class);
    }

    @Test
    void allowsAFixedAmountWhenNoActiveProductExistsInScopeYet() {
        findCheapestActiveBasePriceUseCase.setCheapestBasePriceAmount(null);

        CreatePricingRuleResult result = service.execute(
            timeBasedCommand(DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(1000)));

        assertThat(result.ruleId()).isNotNull();
    }

    @Test
    void validatesAFixedAmountAgainstTheSpecificProductWhenScopedToOneProduct() {
        ProductId productId = ProductId.newId();
        getProductUseCase.register(new GetProductResult(
            productId, "SKU-1", BigDecimal.valueOf(5), "EUR", "URBAN", "ACTIVE"));

        CreatePricingRuleCommand command = new CreatePricingRuleCommand(
            PricingRuleType.TIME_BASED, LocalTime.of(22, 0), LocalTime.of(6, 0),
            null, null, null, DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(5), 10,
            CombinationMode.STACKABLE, productId, null);

        assertThatThrownBy(() -> service.execute(command)).isInstanceOf(InvalidDiscountException.class);
    }
}
