package com.priceforge.audit.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditEntryTest {

    private static final Instant NOW = Instant.parse("2026-07-20T10:00:00Z");

    @Test
    void aPriceCalculationEntryMayCarryItsSnapshot() {
        PriceCalculationSnapshot snapshot = snapshot();

        AuditEntry entry = new AuditEntry(
            UUID.randomUUID(), "PriceCalculated", AuditEntityType.PRICE_CALCULATION, "p-1", "p-1", "u-1",
            null, null, null, null, "{}", NOW, snapshot);

        assertThat(entry.priceSnapshot()).isEqualTo(snapshot);
    }

    @Test
    void anEntryOfAnotherTypeCannotCarryAPriceSnapshot() {
        assertThatThrownBy(() -> new AuditEntry(
                UUID.randomUUID(), "ProductCreated", AuditEntityType.PRODUCT, "p-1", "p-1", null,
                null, null, null, null, "{}", NOW, snapshot()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("PRICE_CALCULATION");
    }

    @Test
    void requiresTheFieldsEveryEntryHas() {
        assertThatThrownBy(() -> new AuditEntry(
                null, "ProductCreated", AuditEntityType.PRODUCT, "p-1", null, null,
                null, null, null, null, "{}", NOW, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("eventId");
        assertThatThrownBy(() -> new AuditEntry(
                UUID.randomUUID(), "ProductCreated", AuditEntityType.PRODUCT, "p-1", null, null,
                null, null, null, null, null, NOW, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("payload");
    }

    private static PriceCalculationSnapshot snapshot() {
        return new PriceCalculationSnapshot(
            "p-1", "u-1", "VIP", 1, NOW, "URBAN", "EUR", "UTC", BigDecimal.TEN, BigDecimal.ONE);
    }
}
