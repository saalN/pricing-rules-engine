package com.priceforge.pricing.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.priceforge.pricing.domain.model.CategoryScope;
import com.priceforge.pricing.domain.model.CombinationMode;
import com.priceforge.pricing.domain.model.CustomerSegment;
import com.priceforge.pricing.domain.model.Discount;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.ExclusionReason;
import com.priceforge.pricing.domain.model.GeoCondition;
import com.priceforge.pricing.domain.model.GlobalScope;
import com.priceforge.pricing.domain.model.PriceQuote;
import com.priceforge.pricing.domain.model.PricingContext;
import com.priceforge.pricing.domain.model.PricingRule;
import com.priceforge.pricing.domain.model.PricingRuleType;
import com.priceforge.pricing.domain.model.ProductScope;
import com.priceforge.pricing.domain.model.RuleCondition;
import com.priceforge.pricing.domain.model.RuleScope;
import com.priceforge.pricing.domain.model.SegmentCondition;
import com.priceforge.pricing.domain.model.VolumeCondition;
import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.PricingRuleId;
import com.priceforge.sharedkernel.ProductId;
import com.priceforge.sharedkernel.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

class PriceCalculatorServiceTest {

    private static final ZoneId UTC = ZoneId.of("UTC");
    private final PriceCalculatorService calculator = new PriceCalculatorService();
    private final ProductId productId = ProductId.newId();

    private PricingContext context() {
        return new PricingContext(
            productId, UserId.newId(), CustomerSegment.VIP, 5, Instant.now(), "URBAN", "EUR", UTC);
    }

    private PricingRule rule(
            RuleCondition condition, RuleScope scope, Discount discount, int priority,
            CombinationMode mode, Instant createdAt) {
        return PricingRule.create(
            PricingRuleId.newId(), PricingRuleType.SEGMENT_BASED, condition, scope, discount, priority, mode, createdAt);
    }

    private RuleCondition alwaysMatches() {
        return new SegmentCondition(CustomerSegment.VIP);
    }

    @Test
    void returnsTheBasePriceUnchangedWhenNoRuleApplies() {
        PriceQuote quote = calculator.calculate(
            Money.of(BigDecimal.valueOf(100), "EUR"), productId, "URBAN", List.of(), context(), false);

        assertThat(amount(quote.finalPrice())).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(quote.appliedRules()).isEmpty();
        assertThat(quote.excludedRules()).isEmpty();
    }

    @Test
    void appliesASingleStackableRule() {
        PricingRule rule = rule(alwaysMatches(), new GlobalScope(),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(20)), 10, CombinationMode.STACKABLE, Instant.now());

        PriceQuote quote = calculator.calculate(
            Money.of(BigDecimal.valueOf(100), "EUR"), productId, "URBAN", List.of(rule), context(), false);

        assertThat(amount(quote.finalPrice())).isEqualByComparingTo(BigDecimal.valueOf(80));
        assertThat(quote.appliedRules()).hasSize(1);
    }

    @Test
    void appliesASingleExclusiveRule() {
        PricingRule rule = rule(alwaysMatches(), new GlobalScope(),
            new Discount(DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(15)), 10, CombinationMode.EXCLUSIVE, Instant.now());

        PriceQuote quote = calculator.calculate(
            Money.of(BigDecimal.valueOf(100), "EUR"), productId, "URBAN", List.of(rule), context(), false);

        assertThat(amount(quote.finalPrice())).isEqualByComparingTo(BigDecimal.valueOf(85));
    }

    @Test
    void exclusiveRuleExcludesLowerPriorityStackableRulesWithoutFlaggingAConflict() {
        Instant now = Instant.now();
        PricingRule highPriorityExclusive = rule(alwaysMatches(), new GlobalScope(),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(30)), 20, CombinationMode.EXCLUSIVE, now);
        PricingRule lowPriorityStackable = rule(alwaysMatches(), new GlobalScope(),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(10)), 10, CombinationMode.STACKABLE, now);

        PriceQuote quote = calculator.calculate(
            Money.of(BigDecimal.valueOf(100), "EUR"), productId, "URBAN",
            List.of(highPriorityExclusive, lowPriorityStackable), context(), false);

        assertThat(amount(quote.finalPrice())).isEqualByComparingTo(BigDecimal.valueOf(70));
        assertThat(quote.appliedRules()).extracting(r -> r.ruleId()).containsExactly(highPriorityExclusive.id());
        assertThat(quote.excludedRules()).hasSize(1);
        assertThat(quote.excludedRules().get(0).ruleId()).isEqualTo(lowPriorityStackable.id());
        assertThat(quote.excludedRules().get(0).reason()).isEqualTo(ExclusionReason.EXCLUDED_BY_HIGHER_PRIORITY);
    }

    @Test
    void appliesStackableRulesAtDifferentPrioritiesInPriorityOrder() {
        Instant now = Instant.now();
        PricingRule higher = rule(alwaysMatches(), new GlobalScope(),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(10)), 20, CombinationMode.STACKABLE, now);
        PricingRule lower = rule(alwaysMatches(), new GlobalScope(),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(10)), 10, CombinationMode.STACKABLE, now);

        PriceQuote quote = calculator.calculate(
            Money.of(BigDecimal.valueOf(100), "EUR"), productId, "URBAN", List.of(lower, higher), context(), false);

        // 100 -> (100 * 0.9) = 90 -> (90 * 0.9) = 81
        assertThat(amount(quote.finalPrice())).isEqualByComparingTo(BigDecimal.valueOf(81));
        assertThat(quote.appliedRules()).hasSize(2);
    }

    @Test
    void tiedStackableRulesAreOrderIndependentEvenMixingPercentageAndFixed() {
        Instant now = Instant.now();
        PricingRule percentage = rule(alwaysMatches(), new GlobalScope(),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(10)), 10, CombinationMode.STACKABLE, now);
        PricingRule fixed = rule(alwaysMatches(), new GlobalScope(),
            new Discount(DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(5)), 10, CombinationMode.STACKABLE, now);

        PriceQuote first = calculator.calculate(
            Money.of(BigDecimal.valueOf(100), "EUR"), productId, "URBAN", List.of(percentage, fixed), context(), false);
        PriceQuote second = calculator.calculate(
            Money.of(BigDecimal.valueOf(100), "EUR"), productId, "URBAN", List.of(fixed, percentage), context(), false);

        // both computed against the same starting price (100): 100 - 10 - 5 = 85, regardless of order
        assertThat(amount(first.finalPrice())).isEqualByComparingTo(BigDecimal.valueOf(85));
        assertThat(amount(second.finalPrice())).isEqualByComparingTo(amount(first.finalPrice()));
    }

    @Test
    void twoStackableRulesOfEqualPriorityAreNotIncompatibleAndBothApply() {
        Instant now = Instant.now();
        PricingRule ruleA = rule(alwaysMatches(), new GlobalScope(),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(10)), 10, CombinationMode.STACKABLE, now);
        PricingRule ruleB = rule(alwaysMatches(), new GlobalScope(),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(5)), 10, CombinationMode.STACKABLE, now);

        PriceQuote quote = calculator.calculate(
            Money.of(BigDecimal.valueOf(100), "EUR"), productId, "URBAN", List.of(ruleA, ruleB), context(), false);

        assertThat(quote.appliedRules()).hasSize(2);
        assertThat(quote.excludedRules()).isEmpty();
    }

    @Test
    void twoExclusiveRulesTiedAtSamePriorityOnlyTheEarliestCreatedApplies() {
        Instant earlier = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant later = Instant.now();
        PricingRule earlierRule = rule(alwaysMatches(), new GlobalScope(),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(10)), 10, CombinationMode.EXCLUSIVE, earlier);
        PricingRule laterRule = rule(alwaysMatches(), new GlobalScope(),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(20)), 10, CombinationMode.EXCLUSIVE, later);

        PriceQuote quote = calculator.calculate(
            Money.of(BigDecimal.valueOf(100), "EUR"), productId, "URBAN",
            List.of(laterRule, earlierRule), context(), false);

        assertThat(quote.appliedRules()).extracting(r -> r.ruleId()).containsExactly(earlierRule.id());
        assertThat(quote.excludedRules()).hasSize(1);
        assertThat(quote.excludedRules().get(0).ruleId()).isEqualTo(laterRule.id());
        assertThat(quote.excludedRules().get(0).reason()).isEqualTo(ExclusionReason.EXCLUDED_BY_TIEBREAK);
    }

    @Test
    void exclusiveBeatsStackableWhenTiedAtSamePriority() {
        Instant now = Instant.now();
        PricingRule exclusiveRule = rule(alwaysMatches(), new GlobalScope(),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(10)), 10, CombinationMode.EXCLUSIVE, now);
        PricingRule stackableRule = rule(alwaysMatches(), new GlobalScope(),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(20)), 10, CombinationMode.STACKABLE, now);

        PriceQuote quote = calculator.calculate(
            Money.of(BigDecimal.valueOf(100), "EUR"), productId, "URBAN",
            List.of(stackableRule, exclusiveRule), context(), false);

        assertThat(quote.appliedRules()).extracting(r -> r.ruleId()).containsExactly(exclusiveRule.id());
        assertThat(quote.excludedRules().get(0).reason()).isEqualTo(ExclusionReason.EXCLUDED_BY_TIEBREAK);
    }

    @Test
    void floorsThePriceAtTheMinimumPositiveAmountInsteadOfGoingToZeroOrBelow() {
        PricingRule rule = rule(alwaysMatches(), new GlobalScope(),
            new Discount(DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(99)), 10, CombinationMode.STACKABLE, Instant.now());

        PriceQuote quote = calculator.calculate(
            Money.of(BigDecimal.valueOf(1), "EUR"), productId, "URBAN", List.of(rule), context(), false);

        assertThat(amount(quote.finalPrice())).isEqualByComparingTo(BigDecimal.valueOf(0.01));
        assertThat(quote.priceFloorApplied()).isTrue();
    }

    @Test
    void ignoresARuleWhoseConditionDoesNotMatch() {
        PricingRule rule = rule(new SegmentCondition(CustomerSegment.STUDENT), new GlobalScope(),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(20)), 10, CombinationMode.STACKABLE, Instant.now());

        PriceQuote quote = calculator.calculate(
            Money.of(BigDecimal.valueOf(100), "EUR"), productId, "URBAN", List.of(rule), context(), false);

        assertThat(amount(quote.finalPrice())).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(quote.appliedRules()).isEmpty();
        assertThat(quote.excludedRules()).isEmpty();
    }

    @Test
    void ignoresARuleScopedToADifferentProduct() {
        PricingRule rule = rule(alwaysMatches(), new ProductScope(ProductId.newId()),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(20)), 10, CombinationMode.STACKABLE, Instant.now());

        PriceQuote quote = calculator.calculate(
            Money.of(BigDecimal.valueOf(100), "EUR"), productId, "URBAN", List.of(rule), context(), false);

        assertThat(amount(quote.finalPrice())).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(quote.appliedRules()).isEmpty();
    }

    @Test
    void appliesARuleScopedToTheMatchingCategory() {
        PricingRule rule = rule(alwaysMatches(), new CategoryScope("URBAN"),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(20)), 10, CombinationMode.STACKABLE, Instant.now());

        PriceQuote quote = calculator.calculate(
            Money.of(BigDecimal.valueOf(100), "EUR"), productId, "URBAN", List.of(rule), context(), false);

        assertThat(amount(quote.finalPrice())).isEqualByComparingTo(BigDecimal.valueOf(80));
    }

    @Test
    void volumeConditionMatchesWhenQuantityMeetsTheMinimum() {
        PricingContext context = new PricingContext(
            productId, UserId.newId(), CustomerSegment.STANDARD, 10, Instant.now(), "URBAN", "EUR", UTC);
        PricingRule rule = rule(new VolumeCondition(10), new GlobalScope(),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(15)), 10, CombinationMode.EXCLUSIVE, Instant.now());

        PriceQuote quote = calculator.calculate(
            Money.of(BigDecimal.valueOf(100), "EUR"), productId, "URBAN", List.of(rule), context, false);

        assertThat(amount(quote.finalPrice())).isEqualByComparingTo(BigDecimal.valueOf(85));
    }

    @Test
    void geoConditionMatchesTheContextLocation() {
        PricingContext context = new PricingContext(
            productId, UserId.newId(), CustomerSegment.STANDARD, 1, Instant.now(), "RURAL", "EUR", UTC);
        PricingRule rule = rule(new GeoCondition("RURAL"), new GlobalScope(),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(10)), 10, CombinationMode.STACKABLE, Instant.now());

        PriceQuote quote = calculator.calculate(
            Money.of(BigDecimal.valueOf(100), "EUR"), productId, "URBAN", List.of(rule), context, false);

        assertThat(amount(quote.finalPrice())).isEqualByComparingTo(BigDecimal.valueOf(90));
    }

    @Test
    void passesTheSimulatedFlagThrough() {
        PriceQuote quote = calculator.calculate(
            Money.of(BigDecimal.valueOf(100), "EUR"), productId, "URBAN", List.of(), context(), true);

        assertThat(quote.simulated()).isTrue();
    }

    private BigDecimal amount(Money money) {
        return money.amount();
    }
}
