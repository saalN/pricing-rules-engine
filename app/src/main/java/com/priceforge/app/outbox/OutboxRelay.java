package com.priceforge.app.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceforge.sharedkernel.DomainEvent;
import com.priceforge.sharedkernel.OutboxEventHandler;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls each bounded context's {@code outbox_event} table for unpublished rows and dispatches
 * them to the {@link OutboxEventHandler}s registered for that event type, then marks the row
 * published — a generic, schema-parameterized relay shared by every context instead of one
 * poller per context.
 *
 * <p>{@code priceforge.outbox.schemas} only lists schemas that actually have an outbox_event
 * table yet; it grows as each bounded context adds its own (currently just catalog).
 */
@Component
public class OutboxRelay {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final List<String> contextSchemas;
    private final Map<String, List<OutboxEventHandler<?>>> handlersByEventType;

    public OutboxRelay(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            @Value("#{'${priceforge.outbox.schemas:catalog}'.split(',')}") List<String> contextSchemas,
            List<OutboxEventHandler<?>> handlers) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.contextSchemas = contextSchemas;
        this.handlersByEventType = handlers.stream()
            .collect(Collectors.groupingBy(OutboxEventHandler::eventType));
    }

    @Scheduled(fixedDelayString = "${priceforge.outbox.relay-interval-ms:5000}")
    public void relay() {
        for (String schema : contextSchemas) {
            relaySchema(schema);
        }
    }

    private void relaySchema(String schema) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT event_id, event_type, payload FROM " + schema
                + ".outbox_event WHERE published_at IS NULL ORDER BY occurred_at");

        for (Map<String, Object> row : rows) {
            dispatch((String) row.get("event_type"), row.get("payload").toString());
            jdbcTemplate.update(
                "UPDATE " + schema + ".outbox_event SET published_at = ? WHERE event_id = ?",
                Timestamp.from(Instant.now()),
                row.get("event_id"));
        }
    }

    private void dispatch(String eventType, String payloadJson) {
        for (OutboxEventHandler<?> handler : handlersByEventType.getOrDefault(eventType, List.of())) {
            invoke(handler, payloadJson);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends DomainEvent> void invoke(OutboxEventHandler<?> handler, String payloadJson) {
        OutboxEventHandler<T> typedHandler = (OutboxEventHandler<T>) handler;
        try {
            T event = objectMapper.readValue(payloadJson, typedHandler.eventClass());
            typedHandler.handle(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize outbox event payload", e);
        }
    }
}
