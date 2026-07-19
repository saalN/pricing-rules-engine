package com.priceforge.pricing.infrastructure.in.outbox;

import com.priceforge.pricing.application.port.out.PriceQuoteCachePort;
import com.priceforge.pricing.infrastructure.in.outbox.event.CatalogProductChangedEvent;
import com.priceforge.sharedkernel.OutboxEventHandler;
import com.priceforge.sharedkernel.ProductId;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProductDeactivatedCacheInvalidationHandler implements OutboxEventHandler<CatalogProductChangedEvent> {

    private final PriceQuoteCachePort cache;

    public ProductDeactivatedCacheInvalidationHandler(PriceQuoteCachePort cache) {
        this.cache = cache;
    }

    @Override
    public String eventType() {
        return "ProductDeactivated";
    }

    @Override
    public Class<CatalogProductChangedEvent> eventClass() {
        return CatalogProductChangedEvent.class;
    }

    @Override
    public void handle(CatalogProductChangedEvent event) {
        cache.evictAllForProduct(new ProductId(UUID.fromString(event.aggregateId())));
    }
}
