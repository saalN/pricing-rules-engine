package com.priceforge.pricing.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.priceforge.catalog.application.dto.GetProductResult;
import com.priceforge.pricing.application.dto.CalculatePriceResult;
import com.priceforge.pricing.application.dto.HypotheticalPricingRule;
import com.priceforge.pricing.application.dto.SimulatePriceCommand;
import com.priceforge.pricing.application.service.support.FakeGetProductUseCase;
import com.priceforge.pricing.application.service.support.InMemoryPricingRuleRepositoryPort;
import com.priceforge.pricing.domain.model.CombinationMode;
import com.priceforge.pricing.domain.model.CustomerSegment;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.PricingRuleType;
import com.priceforge.sharedkernel.ProductId;
import com.priceforge.sharedkernel.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class SimulatePriceServiceTest {

    private final FakeGetProductUseCase getProductUseCase = new FakeGetProductUseCase();
    private final InMemoryPricingRuleRepositoryPort pricingRuleRepository = new InMemoryPricingRuleRepositoryPort();
    private final SimulatePriceService service =
        new SimulatePriceService(getProductUseCase, pricingRuleRepository);
    private final ProductId productId = ProductId.newId();

    @Test
    void simulatesWithAHypotheticalRuleWithoutPersistingIt() {
        getProductUseCase.register(new GetProductResult(
            productId, "SKU-1", BigDecimal.valueOf(100), "EUR", "URBAN", "ACTIVE"));

        HypotheticalPricingRule hypotheticalRule = new HypotheticalPricingRule(
            PricingRuleType.SEGMENT_BASED, null, null, CustomerSegment.VIP, null, null,
            DiscountType.PERCENTAGE, BigDecimal.valueOf(15), 10, CombinationMode.STACKABLE, null, null);

        SimulatePriceCommand command = new SimulatePriceCommand(
            productId, UserId.newId(), CustomerSegment.VIP, 1, Instant.now(), "URBAN", "EUR",
            ZoneId.of("UTC"), hypotheticalRule);

        CalculatePriceResult result = service.execute(command);

        assertThat(result.finalPriceAmount()).isEqualByComparingTo(BigDecimal.valueOf(85));
        assertThat(result.simulated()).isTrue();
        assertThat(pricingRuleRepository.findAllActive()).isEmpty();
    }

    @Test
    void simulatesWithOnlyCurrentlyActiveRulesWhenNoHypotheticalRuleIsGiven() {
        getProductUseCase.register(new GetProductResult(
            productId, "SKU-1", BigDecimal.valueOf(100), "EUR", "URBAN", "ACTIVE"));

        SimulatePriceCommand command = new SimulatePriceCommand(
            productId, UserId.newId(), CustomerSegment.VIP, 1, Instant.now(), "URBAN", "EUR", ZoneId.of("UTC"), null);

        CalculatePriceResult result = service.execute(command);

        assertThat(result.finalPriceAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }
}
