package com.priceforge.sharedkernel;

import java.time.Instant;
import java.util.UUID;

/**
 * Base contract for every domain event published across bounded contexts via the outbox.
 */
public interface DomainEvent {

    UUID eventId();

    Instant occurredAt();

    String aggregateId();
}
