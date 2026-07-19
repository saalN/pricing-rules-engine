package com.priceforge.pricing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.catalog.application.dto.GetProductResult;
import com.priceforge.pricing.application.dto.CalculatePriceCommand;
import com.priceforge.pricing.application.dto.CalculatePriceResult;
import com.priceforge.pricing.application.service.support.FakeGetProductUseCase;
import com.priceforge.pricing.application.service.support.InMemoryPriceQuoteCachePort;
import com.priceforge.pricing.application.service.support.InMemoryPricingRuleRepositoryPort;
import com.priceforge.pricing.application.service.support.RecordingEventPublisher;
import com.priceforge.pricing.domain.exception.CurrencyMismatchException;
import com.priceforge.pricing.domain.exception.ProductNotActiveException;
import com.priceforge.pricing.domain.exception.ProductNotFoundException;
import com.priceforge.pricing.domain.model.CombinationMode;
import com.priceforge.pricing.domain.model.CustomerSegment;
import com.priceforge.pricing.domain.model.Discount;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.GlobalScope;
import com.priceforge.pricing.domain.model.PriceCalculated;
import com.priceforge.pricing.domain.model.PricingRule;
import com.priceforge.pricing.domain.model.PricingRuleType;
import com.priceforge.pricing.domain.model.RuleConflictDetected;
import com.priceforge.pricing.domain.model.SegmentCondition;
import com.priceforge.sharedkernel.PricingRuleId;
import com.priceforge.sharedkernel.ProductId;
import com.priceforge.sharedkernel.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class CalculatePriceServiceTest {

    private final FakeGetProductUseCase getProductUseCase = new FakeGetProductUseCase();
    private final InMemoryPricingRuleRepositoryPort pricingRuleRepository = new InMemoryPricingRuleRepositoryPort();
    private final InMemoryPriceQuoteCachePort cache = new InMemoryPriceQuoteCachePort();
    private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
    private final CalculatePriceService service =
        new CalculatePriceService(getProductUseCase, pricingRuleRepository, cache, eventPublisher);

    private final ProductId productId = ProductId.newId();

    private void registerProduct(String currency, String status) {
        getProductUseCase.register(new GetProductResult(
            productId, "SKU-1", BigDecimal.valueOf(100), currency, "URBAN", status));
    }

    private CalculatePriceCommand command(String currency) {
        return new CalculatePriceCommand(
            productId, UserId.newId(), CustomerSegment.VIP, 1, Instant.now(), "URBAN", currency, ZoneId.of("UTC"));
    }

    @Test
    void calculatesThePriceAndPublishesPriceCalculated() {
        registerProduct("EUR", "ACTIVE");
        pricingRuleRepository.save(PricingRule.create(
            PricingRuleId.newId(), PricingRuleType.SEGMENT_BASED, new SegmentCondition(CustomerSegment.VIP),
            new GlobalScope(), new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(10)),
            10, CombinationMode.STACKABLE, Instant.now()));

        CalculatePriceResult result = service.execute(command("EUR"));

        assertThat(result.finalPriceAmount()).isEqualByComparingTo(BigDecimal.valueOf(90));
        assertThat(eventPublisher.publishedEvents()).anyMatch(PriceCalculated.class::isInstance);
    }

    @Test
    void rejectsWhenTheProductDoesNotExist() {
        assertThatThrownBy(() -> service.execute(command("EUR"))).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void rejectsWhenTheProductIsNotActive() {
        registerProduct("EUR", "INACTIVE");

        assertThatThrownBy(() -> service.execute(command("EUR"))).isInstanceOf(ProductNotActiveException.class);
    }

    @Test
    void rejectsACurrencyThatDoesNotMatchTheProduct() {
        registerProduct("EUR", "ACTIVE");

        assertThatThrownBy(() -> service.execute(command("USD"))).isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void reusesACachedQuoteInsteadOfRecalculating() {
        registerProduct("EUR", "ACTIVE");
        CalculatePriceCommand command = command("EUR");

        service.execute(command);
        int eventsAfterFirstCall = eventPublisher.publishedEvents().size();
        service.execute(command);

        assertThat(eventPublisher.publishedEvents()).hasSize(eventsAfterFirstCall);
    }

    @Test
    void publishesRuleConflictDetectedWhenThePriceIsFloored() {
        registerProduct("EUR", "ACTIVE");
        pricingRuleRepository.save(PricingRule.create(
            PricingRuleId.newId(), PricingRuleType.SEGMENT_BASED, new SegmentCondition(CustomerSegment.VIP),
            new GlobalScope(), new Discount(DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(150)),
            10, CombinationMode.STACKABLE, Instant.now()));

        service.execute(command("EUR"));

        assertThat(eventPublisher.publishedEvents()).anyMatch(RuleConflictDetected.class::isInstance);
    }
}
