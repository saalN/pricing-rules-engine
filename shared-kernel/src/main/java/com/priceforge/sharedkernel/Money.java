package com.priceforge.sharedkernel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Amount plus an ISO 4217 currency. Purely a value object: whether a negative or zero amount is
 * allowed is a rule of the aggregate using it, not of {@code Money} itself.
 */
public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money multipliedBy(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor must not be null");
        return new Money(amount.multiply(factor), currency);
    }

    // @JsonIgnore: these are query methods, not data — without it, Jackson's default bean-property
    // introspection serializes them as extra "negative"/"zero" JSON properties (since they follow
    // the isXxx() getter convention), which then breaks deserialization back into this record.
    @JsonIgnore
    public boolean isNegative() {
        return amount.signum() < 0;
    }

    @JsonIgnore
    public boolean isZero() {
        return amount.signum() == 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Cannot operate on Money with different currencies: " + currency + " vs " + other.currency);
        }
    }
}
