package com.priceforge.pricing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.catalog.application.dto.GetProductResult;
import com.priceforge.pricing.application.dto.ReplayPriceCalculationCommand;
import com.priceforge.pricing.application.dto.ReplayPriceCalculationResult;
import com.priceforge.pricing.application.service.support.FakeGetProductUseCase;
import com.priceforge.pricing.application.service.support.InMemoryPricingRuleRepositoryPort;
import com.priceforge.pricing.application.service.support.RuleDefinitionMapper;
import com.priceforge.pricing.domain.exception.CurrencyMismatchException;
import com.priceforge.pricing.domain.exception.ProductNotFoundException;
import com.priceforge.sharedkernel.CustomerSegment;
import com.priceforge.pricing.domain.model.Discount;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.PricingRule;
import com.priceforge.pricing.domain.model.PricingRuleType;
import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.sharedkernel.PricingRuleId;
import com.priceforge.sharedkernel.ProductId;
import com.priceforge.sharedkernel.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class ReplayPriceCalculationServiceTest {

    private final FakeGetProductUseCase getProductUseCase = new FakeGetProductUseCase();
    private final InMemoryPricingRuleRepositoryPort pricingRuleRepository = new InMemoryPricingRuleRepositoryPort();
    private final ReplayPriceCalculationService service =
        new ReplayPriceCalculationService(getProductUseCase, pricingRuleRepository);
    private final ProductId productId = ProductId.newId();

    @Test
    void replaysAStoredContextAgainstTheCurrentlyActiveRules() {
        getProductUseCase.register(product("ACTIVE", "EUR"));
        pricingRuleRepository.save(vipTenPercentRule());

        ReplayPriceCalculationResult result = service.execute(command("VIP", "EUR"));

        assertThat(result.basePriceAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(result.finalPriceAmount()).isEqualByComparingTo(BigDecimal.valueOf(90));
        assertThat(result.currency()).isEqualTo("EUR");
    }

    @Test
    void doesNotApplyARuleWhoseConditionTheStoredContextDoesNotMeet() {
        getProductUseCase.register(product("ACTIVE", "EUR"));
        pricingRuleRepository.save(vipTenPercentRule());

        ReplayPriceCalculationResult result = service.execute(command("STANDARD", "EUR"));

        assertThat(result.finalPriceAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void stillReplaysACalculationForAProductDeactivatedSince() {
        getProductUseCase.register(product("INACTIVE", "EUR"));

        ReplayPriceCalculationResult result = service.execute(command("VIP", "EUR"));

        assertThat(result.finalPriceAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void rejectsAProductThatNoLongerExists() {
        assertThatThrownBy(() -> service.execute(command("VIP", "EUR")))
            .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void rejectsAContextWhoseCurrencyDiffersFromTheProducts() {
        getProductUseCase.register(product("ACTIVE", "USD"));

        assertThatThrownBy(() -> service.execute(command("VIP", "EUR")))
            .isInstanceOf(CurrencyMismatchException.class);
    }

    private GetProductResult product(String status, String currency) {
        return new GetProductResult(productId, "SKU-1", BigDecimal.valueOf(100), currency, "URBAN", status);
    }

    private ReplayPriceCalculationCommand command(String segment, String currency) {
        return new ReplayPriceCalculationCommand(
            productId, UserId.newId(), segment, 1, Instant.now(), "URBAN", currency, ZoneId.of("UTC"));
    }

    private PricingRule vipTenPercentRule() {
        return PricingRule.create(
            PricingRuleId.newId(),
            PricingRuleType.SEGMENT_BASED,
            RuleDefinitionMapper.toCondition(
                PricingRuleType.SEGMENT_BASED, null, null, CustomerSegment.VIP, null, null),
            RuleDefinitionMapper.toScope(null, null),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.TEN),
            10,
            CombinationMode.STACKABLE,
            Instant.now());
    }
}
