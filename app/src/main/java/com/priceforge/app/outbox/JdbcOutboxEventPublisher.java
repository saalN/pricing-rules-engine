package com.priceforge.app.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceforge.sharedkernel.DomainEvent;
import com.priceforge.sharedkernel.EventPublisher;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Writes a domain event as a row in {@code <schema>.outbox_event}, one instance per bounded
 * context schema (see {@code OutboxConfig}) — never a single cross-context instance, since each
 * context owns its own outbox table.
 */
public class JdbcOutboxEventPublisher implements EventPublisher {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final String schema;

    public JdbcOutboxEventPublisher(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, String schema) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.schema = schema;
    }

    @Override
    public void publish(DomainEvent event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize domain event " + event, e);
        }

        jdbcTemplate.update(
            "INSERT INTO " + schema + ".outbox_event "
                + "(event_id, aggregate_id, event_type, payload, occurred_at) "
                + "VALUES (?, ?, ?, ?::jsonb, ?)",
            event.eventId(),
            event.aggregateId(),
            event.getClass().getSimpleName(),
            payload,
            Timestamp.from(event.occurredAt()));
    }
}
