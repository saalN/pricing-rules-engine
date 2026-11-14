package com.priceforge.pricing.application.port.out;

import com.priceforge.pricing.domain.model.PriceQuote;
import com.priceforge.sharedkernel.ProductId;
import java.util.Optional;

/**
 * UC-06's Redis cache, namespaced {@code pricing:quote:{productId}:{contextHash}} with a short
 * TTL — the adapter owns key formatting and TTL, this port only
 * expresses the semantic operations.
 */
public interface PriceQuoteCachePort {

    Optional<PriceQuote> get(ProductId productId, String contextHash);

    void put(ProductId productId, String contextHash, PriceQuote quote);

    /** Invalidation trigger for Catalog's product events (base price/category/deactivation changes). */
    void evictAllForProduct(ProductId productId);
}
