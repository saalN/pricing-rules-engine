package com.priceforge.audit.infrastructure.in.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.priceforge.audit.infrastructure.in.outbox.event.RawDomainEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RawDomainEventDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void liftsTheDomainEventFieldsAndKeepsTheWholePayload() throws Exception {
        UUID eventId = UUID.randomUUID();

        RawDomainEvent event = objectMapper.readValue("""
            {"eventId": "%s", "occurredAt": "2026-07-20T10:00:00.123456Z", "aggregateId": "agg-1",
             "extra": {"nested": [1, 2]}}
            """.formatted(eventId), RawDomainEvent.class);

        assertThat(event.eventId()).isEqualTo(eventId);
        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-07-20T10:00:00.123456Z"));
        assertThat(event.aggregateId()).isEqualTo("agg-1");
        assertThat(event.payload().get("extra").get("nested")).hasSize(2);
    }

    @Test
    void keepsDecimalsExactInsteadOfRoundingThroughDouble() throws Exception {
        RawDomainEvent event = objectMapper.readValue("""
            {"eventId": "%s", "occurredAt": "2026-07-20T10:00:00Z", "aggregateId": "agg-1",
             "amount": 0.1234567890123456789}
            """.formatted(UUID.randomUUID()), RawDomainEvent.class);

        assertThat(new BigDecimal(event.text("amount"))).isEqualByComparingTo("0.1234567890123456789");
    }

    @Test
    void keepsTrailingZerosInsteadOfNormalisingToScientificNotation() throws Exception {
        RawDomainEvent event = objectMapper.readValue("""
            {"eventId": "%s", "occurredAt": "2026-07-20T10:00:00Z", "aggregateId": "agg-1",
             "amount": 100.0000, "whole": 100}
            """.formatted(UUID.randomUUID()), RawDomainEvent.class);

        assertThat(event.text("amount")).isEqualTo("100.0000");
        assertThat(event.text("whole")).isEqualTo("100");
        assertThat(event.payload().toString()).contains("\"amount\":100.0000").doesNotContain("E+");
    }

    @Test
    void rejectsAPayloadMissingTheDomainEventFields() {
        assertThatThrownBy(() -> objectMapper.readValue("{\"aggregateId\": \"agg-1\"}", RawDomainEvent.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("eventId");
    }
}
