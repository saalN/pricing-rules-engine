package com.priceforge.pricing.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.pricing.domain.exception.PricingRuleNotActiveException;
import com.priceforge.sharedkernel.PricingRuleId;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PricingRuleTest {

    private static PricingRule activeRule() {
        return PricingRule.create(
            PricingRuleId.newId(),
            PricingRuleType.TIME_BASED,
            new TimeWindowCondition(java.time.LocalTime.of(22, 0), java.time.LocalTime.of(6, 0)),
            new GlobalScope(),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(20)),
            10,
            CombinationMode.STACKABLE,
            Instant.now());
    }

    @Test
    void createsAnActiveRule() {
        PricingRule rule = activeRule();

        assertThat(rule.status()).isEqualTo(PricingRuleStatus.ACTIVE);
        assertThat(rule.priority()).isEqualTo(10);
    }

    @Test
    void updatesAnActiveRule() {
        PricingRule rule = activeRule();

        rule.update(
            new TimeWindowCondition(java.time.LocalTime.of(23, 0), java.time.LocalTime.of(5, 0)),
            new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(30)),
            20,
            CombinationMode.EXCLUSIVE);

        assertThat(rule.priority()).isEqualTo(20);
        assertThat(rule.combinationMode()).isEqualTo(CombinationMode.EXCLUSIVE);
        assertThat(rule.discount().value()).isEqualByComparingTo(BigDecimal.valueOf(30));
    }

    @Test
    void deactivatesAnActiveRule() {
        PricingRule rule = activeRule();

        rule.deactivate();

        assertThat(rule.status()).isEqualTo(PricingRuleStatus.INACTIVE);
    }

    @Test
    void rejectsUpdatingAnInactiveRule() {
        PricingRule rule = activeRule();
        rule.deactivate();

        assertThatThrownBy(() -> rule.update(
                rule.condition(), rule.discount(), 5, CombinationMode.STACKABLE))
            .isInstanceOf(PricingRuleNotActiveException.class);
    }

    @Test
    void rejectsDeactivatingAnAlreadyInactiveRule() {
        PricingRule rule = activeRule();
        rule.deactivate();

        assertThatThrownBy(rule::deactivate).isInstanceOf(PricingRuleNotActiveException.class);
    }
}
