package com.priceforge.promotions.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.priceforge.sharedkernel.CustomerSegment;
import com.priceforge.sharedkernel.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EligibilityCriteriaTest {

    private static final Money PRICE = Money.of(BigDecimal.valueOf(100), "EUR");

    @Test
    void matchesEverythingWhenNoConstraintIsSet() {
        EligibilityCriteria criteria = new EligibilityCriteria(null, null, null);

        assertThat(criteria.matches(CustomerSegment.STANDARD, "URBAN", PRICE)).isTrue();
    }

    @Test
    void rejectsAMismatchedSegment() {
        EligibilityCriteria criteria = new EligibilityCriteria(CustomerSegment.VIP, null, null);

        assertThat(criteria.matches(CustomerSegment.STANDARD, "URBAN", PRICE)).isFalse();
        assertThat(criteria.matches(CustomerSegment.VIP, "URBAN", PRICE)).isTrue();
    }

    @Test
    void rejectsAMismatchedCategoryCaseInsensitively() {
        EligibilityCriteria criteria = new EligibilityCriteria(null, "urban", null);

        assertThat(criteria.matches(CustomerSegment.STANDARD, "URBAN", PRICE)).isTrue();
        assertThat(criteria.matches(CustomerSegment.STANDARD, "RURAL", PRICE)).isFalse();
    }

    @Test
    void rejectsAPriceBelowTheMinimumPurchase() {
        EligibilityCriteria criteria = new EligibilityCriteria(null, null, BigDecimal.valueOf(150));

        assertThat(criteria.matches(CustomerSegment.STANDARD, "URBAN", PRICE)).isFalse();
        assertThat(criteria.matches(CustomerSegment.STANDARD, "URBAN", Money.of(BigDecimal.valueOf(200), "EUR")))
            .isTrue();
    }
}
