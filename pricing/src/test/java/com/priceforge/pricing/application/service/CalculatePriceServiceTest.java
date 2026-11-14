package com.priceforge.pricing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.catalog.application.dto.GetProductResult;
import com.priceforge.pricing.application.dto.CalculatePriceCommand;
import com.priceforge.pricing.application.dto.CalculatePriceResult;
import com.priceforge.pricing.application.service.support.FakeGetProductUseCase;
import com.priceforge.pricing.application.service.support.InMemoryPriceQuoteCachePort;
import com.priceforge.pricing.application.service.support.InMemoryPricingRuleRepositoryPort;
import com.priceforge.pricing.application.service.support.NoOpPromotionApplierPort;
import com.priceforge.pricing.application.service.support.RecordingEventPublisher;
import com.priceforge.pricing.domain.exception.CurrencyMismatchException;
import com.priceforge.pricing.domain.exception.ProductNotActiveException;
import com.priceforge.pricing.domain.exception.ProductNotFoundException;
import com.priceforge.pricing.application.port.out.PromotionApplierPort;
import com.priceforge.sharedkernel.AppliedPromotionDetail;
import com.priceforge.sharedkernel.CustomerSegment;
import com.priceforge.pricing.domain.model.Discount;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.GlobalScope;
import com.priceforge.pricing.domain.model.PriceCalculated;
import com.priceforge.pricing.domain.model.PricingRule;
import com.priceforge.pricing.domain.model.PricingRuleType;
import com.priceforge.sharedkernel.PromotionApplicationOutcome;
import com.priceforge.pricing.domain.model.RuleConflictDetected;
import com.priceforge.pricing.domain.model.SegmentCondition;
import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.PricingRuleId;
import com.priceforge.sharedkernel.ProductId;
import com.priceforge.sharedkernel.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculatePriceServiceTest {

    private final FakeGetProductUseCase getProductUseCase = new FakeGetProductUseCase();
    private final InMemoryPricingRuleRepositoryPort pricingRuleRepository = new InMemoryPricingRuleRepositoryPort();
    private final InMemoryPriceQuoteCachePort cache = new InMemoryPriceQuoteCachePort();
    private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
    private final CalculatePriceService service = new CalculatePriceService(
        getProductUseCase, pricingRuleRepository, cache, new NoOpPromotionApplierPort(), eventPublisher);

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
    void publishesPriceCalculatedOnEveryCallButDoesNotRepublishConflictsOnACacheHit() {
        registerProduct("EUR", "ACTIVE");
        pricingRuleRepository.save(PricingRule.create(
            PricingRuleId.newId(), PricingRuleType.SEGMENT_BASED, new SegmentCondition(CustomerSegment.VIP),
            new GlobalScope(), new Discount(DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(150)),
            10, CombinationMode.STACKABLE, Instant.now()));
        CalculatePriceCommand command = command("EUR");

        service.execute(command);
        service.execute(command);

        long priceCalculatedCount = eventPublisher.publishedEvents().stream()
            .filter(PriceCalculated.class::isInstance).count();
        long conflictCount = eventPublisher.publishedEvents().stream()
            .filter(RuleConflictDetected.class::isInstance).count();

        // one PriceCalculated per call (each call is a distinct auditable quote)...
        assertThat(priceCalculatedCount).isEqualTo(2);
        // ...but the conflict was only detected once, when the pricing rules were actually computed.
        assertThat(conflictCount).isEqualTo(1);
    }

    @Test
    void mergesThePromotionOutcomeIntoTheFinalQuote() {
        registerProduct("EUR", "ACTIVE");
        PromotionApplierPort applier = (currentPrice, context, forProductId, category) -> new PromotionApplicationOutcome(
            currentPrice.minus(Money.of(BigDecimal.TEN, "EUR")),
            List.of(new AppliedPromotionDetail("promo-1", "10 EUR off", Money.of(BigDecimal.TEN, "EUR"), "eligible")),
            List.of(),
            false);
        CalculatePriceService serviceWithPromotion = new CalculatePriceService(
            getProductUseCase, pricingRuleRepository, cache, applier, eventPublisher);

        CalculatePriceResult result = serviceWithPromotion.execute(command("EUR"));

        assertThat(result.finalPriceAmount()).isEqualByComparingTo(BigDecimal.valueOf(90));
        assertThat(result.appliedPromotions()).hasSize(1);
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
