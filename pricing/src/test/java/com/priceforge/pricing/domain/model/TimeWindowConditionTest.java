package com.priceforge.pricing.domain.model;

import com.priceforge.sharedkernel.CustomerSegment;
import com.priceforge.sharedkernel.PricingContext;
import static org.assertj.core.api.Assertions.assertThat;

import com.priceforge.sharedkernel.ProductId;
import com.priceforge.sharedkernel.UserId;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class TimeWindowConditionTest {

    private static final ZoneId UTC = ZoneId.of("UTC");

    private PricingContext contextAt(LocalTime time) {
        return new PricingContext(
            ProductId.newId(), UserId.newId(), CustomerSegment.STANDARD, 1,
            ZonedDateTime.of(2026, 1, 1, time.getHour(), time.getMinute(), 0, 0, UTC).toInstant(),
            "URBAN", "EUR", UTC);
    }

    @Test
    void matchesInsideANonWrappingWindow() {
        TimeWindowCondition condition = new TimeWindowCondition(LocalTime.of(9, 0), LocalTime.of(17, 0));

        assertThat(condition.matches(contextAt(LocalTime.of(12, 0)))).isTrue();
        assertThat(condition.matches(contextAt(LocalTime.of(8, 59)))).isFalse();
        assertThat(condition.matches(contextAt(LocalTime.of(17, 0)))).isFalse();
    }

    @Test
    void matchesInsideAWrappingWindow() {
        TimeWindowCondition condition = new TimeWindowCondition(LocalTime.of(22, 0), LocalTime.of(6, 0));

        assertThat(condition.matches(contextAt(LocalTime.of(23, 0)))).isTrue();
        assertThat(condition.matches(contextAt(LocalTime.of(2, 0)))).isTrue();
        assertThat(condition.matches(contextAt(LocalTime.of(12, 0)))).isFalse();
    }
}
