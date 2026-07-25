package com.priceforge.pricing.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.priceforge.pricing.application.dto.CalculateWorstCasePricingDiscountQuery;
import com.priceforge.pricing.application.dto.CalculateWorstCasePricingDiscountResult;
import com.priceforge.pricing.application.service.support.InMemoryPricingRuleRepositoryPort;
import com.priceforge.pricing.domain.model.CategoryScope;
import com.priceforge.pricing.domain.model.Discount;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.GlobalScope;
import com.priceforge.pricing.domain.model.PricingRule;
import com.priceforge.pricing.domain.model.PricingRuleType;
import com.priceforge.pricing.domain.model.ProductScope;
import com.priceforge.pricing.domain.model.RuleScope;
import com.priceforge.pricing.domain.model.SegmentCondition;
import com.priceforge.pricing.domain.model.CustomerSegment;
import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.sharedkernel.PricingRuleId;
import com.priceforge.sharedkernel.ProductId;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CalculateWorstCasePricingDiscountServiceTest {

    private final InMemoryPricingRuleRepositoryPort repository = new InMemoryPricingRuleRepositoryPort();
    private final CalculateWorstCasePricingDiscountService service =
        new CalculateWorstCasePricingDiscountService(repository);

    private PricingRule rule(RuleScopeKind scope, CombinationMode mode, DiscountType type, BigDecimal value) {
        return PricingRule.create(
            PricingRuleId.newId(), PricingRuleType.SEGMENT_BASED, new SegmentCondition(CustomerSegment.VIP),
            scope.toScope(), new Discount(type, value), 10, mode, Instant.now());
    }

    private enum RuleScopeKind {
        GLOBAL, URBAN_CATEGORY, RURAL_CATEGORY, SPECIFIC_PRODUCT;

        RuleScope toScope() {
            return switch (this) {
                case GLOBAL -> new GlobalScope();
                case URBAN_CATEGORY -> new CategoryScope("URBAN");
                case RURAL_CATEGORY -> new CategoryScope("RURAL");
                case SPECIFIC_PRODUCT -> new ProductScope(ProductId.newId());
            };
        }
    }

    @Test
    void sumsStackableRulesInScope() {
        repository.save(rule(RuleScopeKind.URBAN_CATEGORY, CombinationMode.STACKABLE, DiscountType.PERCENTAGE, BigDecimal.TEN));
        repository.save(rule(RuleScopeKind.GLOBAL, CombinationMode.STACKABLE, DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(5)));

        CalculateWorstCasePricingDiscountResult result = service.execute(
            new CalculateWorstCasePricingDiscountQuery(BigDecimal.valueOf(100), "EUR", "URBAN"));

        // 10% of 100 = 10, plus a flat 5 = 15
        assertThat(result.worstCaseDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(15));
    }

    @Test
    void takesTheGreaterOfTheStackableSumAndTheExclusiveMax() {
        repository.save(rule(RuleScopeKind.URBAN_CATEGORY, CombinationMode.STACKABLE, DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(5)));
        repository.save(rule(RuleScopeKind.URBAN_CATEGORY, CombinationMode.EXCLUSIVE, DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(40)));
        repository.save(rule(RuleScopeKind.URBAN_CATEGORY, CombinationMode.EXCLUSIVE, DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(20)));

        CalculateWorstCasePricingDiscountResult result = service.execute(
            new CalculateWorstCasePricingDiscountQuery(BigDecimal.valueOf(100), "EUR", "URBAN"));

        // stackable sum = 5; exclusive max = 40; take the greater
        assertThat(result.worstCaseDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(40));
    }

    @Test
    void ignoresRulesScopedToADifferentCategory() {
        repository.save(rule(RuleScopeKind.RURAL_CATEGORY, CombinationMode.STACKABLE, DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(50)));

        CalculateWorstCasePricingDiscountResult result = service.execute(
            new CalculateWorstCasePricingDiscountQuery(BigDecimal.valueOf(100), "EUR", "URBAN"));

        assertThat(result.worstCaseDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void ignoresProductScopedRulesSinceNoConcreteProductIsKnown() {
        repository.save(rule(RuleScopeKind.SPECIFIC_PRODUCT, CombinationMode.EXCLUSIVE, DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(90)));

        CalculateWorstCasePricingDiscountResult result = service.execute(
            new CalculateWorstCasePricingDiscountQuery(BigDecimal.valueOf(100), "EUR", "URBAN"));

        assertThat(result.worstCaseDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void includesEveryCategoryScopedRuleWhenTheQueryIsGlobal() {
        repository.save(rule(RuleScopeKind.URBAN_CATEGORY, CombinationMode.STACKABLE, DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(10)));
        repository.save(rule(RuleScopeKind.RURAL_CATEGORY, CombinationMode.STACKABLE, DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(20)));

        CalculateWorstCasePricingDiscountResult result = service.execute(
            new CalculateWorstCasePricingDiscountQuery(BigDecimal.valueOf(100), "EUR", null));

        assertThat(result.worstCaseDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(30));
    }
}
