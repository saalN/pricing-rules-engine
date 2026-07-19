package com.priceforge.sharedkernel;

/**
 * Registered by a bounded context to react to another context's domain event, delivered
 * at-least-once by the generic outbox relay (see {@code app.outbox.OutboxRelay}). The event type
 * name matches the emitting event's simple class name, as written by the JDBC outbox publisher.
 *
 * <p>Lives in shared-kernel rather than in {@code app} because a consuming context (e.g. Pricing
 * reacting to Catalog's events) must be able to implement this port without depending on
 * {@code app} — that dependency would point the wrong way.
 */
public interface OutboxEventHandler<T extends DomainEvent> {

    String eventType();

    Class<T> eventClass();

    void handle(T event);
}
