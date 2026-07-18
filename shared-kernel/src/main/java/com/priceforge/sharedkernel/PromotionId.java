package com.priceforge.sharedkernel;

import java.util.Objects;
import java.util.UUID;

public record PromotionId(UUID value) {

    public PromotionId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static PromotionId newId() {
        return new PromotionId(UUID.randomUUID());
    }

    public static PromotionId of(String value) {
        return new PromotionId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
