package com.priceforge.pricing.infrastructure.out.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceforge.pricing.application.port.out.PriceQuoteCachePort;
import com.priceforge.pricing.domain.model.PriceQuote;
import com.priceforge.sharedkernel.ProductId;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Namespaced {@code pricing:quote:{productId}:{contextHash}} with a short TTL
 * (arquitectura-tecnica.md). Caching is best-effort: a serialization failure on write is
 * swallowed rather than failing the calculation, and a corrupt/unreadable entry on read is
 * treated as a cache miss.
 */
@Repository
public class RedisPriceQuoteCacheAdapter implements PriceQuoteCachePort {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RedisPriceQuoteCacheAdapter(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${priceforge.pricing.cache.ttl-seconds:60}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    @Override
    public Optional<PriceQuote> get(ProductId productId, String contextHash) {
        String json = redisTemplate.opsForValue().get(key(productId, contextHash));
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, PriceQuote.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    @Override
    public void put(ProductId productId, String contextHash, PriceQuote quote) {
        try {
            redisTemplate.opsForValue().set(key(productId, contextHash), objectMapper.writeValueAsString(quote), ttl);
        } catch (JsonProcessingException e) {
            // Caching is an optimization, not a correctness requirement — never fail the
            // calculation because the result couldn't be cached.
        }
    }

    @Override
    public void evictAllForProduct(ProductId productId) {
        Set<String> keys = redisTemplate.keys("pricing:quote:" + productId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private String key(ProductId productId, String contextHash) {
        return "pricing:quote:" + productId + ":" + contextHash;
    }
}
