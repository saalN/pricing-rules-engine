package com.priceforge.promotions.application.service.support;

import com.priceforge.sharedkernel.DomainEvent;
import com.priceforge.sharedkernel.EventPublisher;
import java.util.ArrayList;
import java.util.List;

public final class RecordingEventPublisher implements EventPublisher {

    private final List<DomainEvent> publishedEvents = new ArrayList<>();

    @Override
    public void publish(DomainEvent event) {
        publishedEvents.add(event);
    }

    public List<DomainEvent> publishedEvents() {
        return publishedEvents;
    }
}
