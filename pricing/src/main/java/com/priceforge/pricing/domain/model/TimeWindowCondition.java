package com.priceforge.pricing.domain.model;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Matches when the context's local time falls within [startTime, endTime). Handles windows that
 * wrap past midnight (e.g. 22:00-06:00): the window is "wrapping" whenever
 * {@code endTime} is not after {@code startTime}.
 */
public record TimeWindowCondition(LocalTime startTime, LocalTime endTime) implements RuleCondition {

    public TimeWindowCondition {
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");
    }

    @Override
    public boolean matches(PricingContext context) {
        LocalTime time = ZonedDateTime.ofInstant(context.requestedAt(), context.zoneId()).toLocalTime();
        if (endTime.isAfter(startTime)) {
            return !time.isBefore(startTime) && time.isBefore(endTime);
        }
        return !time.isBefore(startTime) || time.isBefore(endTime);
    }

    @Override
    public String describe() {
        return "time window " + startTime + "-" + endTime;
    }
}
