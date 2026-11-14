package com.priceforge.audit.infrastructure.in.outbox.event;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

/**
 * Reads the payload as a tree with decimals kept exact — neither rounded through {@code double}
 * nor normalised by stripping trailing zeros ({@code 100.0000} would otherwise become
 * {@code 1E+2} in the stored payload and in every report built from it) — and lifts the
 * {@code DomainEvent} fields out of it.
 */
class RawDomainEventDeserializer extends StdDeserializer<RawDomainEvent> {

    RawDomainEventDeserializer() {
        super(RawDomainEvent.class);
    }

    @Override
    public RawDomainEvent deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode payload = mapper.readerFor(JsonNode.class)
            .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .without(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)
            .readTree(parser);

        return new RawDomainEvent(
            UUID.fromString(required(payload, "eventId")),
            Instant.parse(required(payload, "occurredAt")),
            required(payload, "aggregateId"),
            payload);
    }

    private static String required(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        if (node == null || node.isNull()) {
            throw new IllegalArgumentException("Domain event payload is missing '" + field + "'");
        }
        return node.asText();
    }
}
