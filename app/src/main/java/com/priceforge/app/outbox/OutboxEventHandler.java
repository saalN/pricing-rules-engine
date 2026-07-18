package com.priceforge.app.outbox;

import com.priceforge.sharedkernel.DomainEvent;

/**
 * Registered by a bounded context to react to another context's domain event, delivered
 * at-least-once by {@link OutboxRelay}. The event type name matches the emitting event's simple
 * class name, as written by {@link JdbcOutboxEventPublisher}.
 */
public interface OutboxEventHandler<T extends DomainEvent> {

    String eventType();

    Class<T> eventClass();

    void handle(T event);
}
