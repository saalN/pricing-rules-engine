package com.priceforge.pricing.application.service.support;

import com.priceforge.pricing.application.port.out.PriceQuoteCachePort;
import com.priceforge.pricing.domain.model.PriceQuote;
import com.priceforge.sharedkernel.ProductId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryPriceQuoteCachePort implements PriceQuoteCachePort {

    private final Map<String, PriceQuote> quotesByKey = new HashMap<>();

    @Override
    public Optional<PriceQuote> get(ProductId productId, String contextHash) {
        return Optional.ofNullable(quotesByKey.get(key(productId, contextHash)));
    }

    @Override
    public void put(ProductId productId, String contextHash, PriceQuote quote) {
        quotesByKey.put(key(productId, contextHash), quote);
    }

    @Override
    public void evictAllForProduct(ProductId productId) {
        quotesByKey.keySet().removeIf(key -> key.startsWith(productId + ":"));
    }

    private String key(ProductId productId, String contextHash) {
        return productId + ":" + contextHash;
    }
}
