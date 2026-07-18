package com.priceforge.sharedkernel;

/**
 * Outbound port used by application services to publish domain events via the outbox.
 */
public interface EventPublisher {

    void publish(DomainEvent event);
}
