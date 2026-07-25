package com.priceforge.pricing.infrastructure.out.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.priceforge.pricing.domain.model.CustomerSegment;
import com.priceforge.pricing.domain.model.PriceQuote;
import com.priceforge.pricing.domain.model.PricingContext;
import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.ProductId;
import com.priceforge.sharedkernel.UserId;
import com.redis.testcontainers.RedisContainer;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataRedisTest
class RedisPriceQuoteCacheAdapterTest {

    private static final RedisContainer REDIS = new RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME);

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final ProductId productId = ProductId.newId();

    private RedisPriceQuoteCacheAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisPriceQuoteCacheAdapter(redisTemplate, objectMapper, 60);
    }

    private PriceQuote sampleQuote() {
        PricingContext context = new PricingContext(
            productId, UserId.newId(), CustomerSegment.VIP, 1, Instant.now(), "URBAN", "EUR", ZoneId.of("UTC"));
        return new PriceQuote(
            Money.of(BigDecimal.TEN, "EUR"), Money.of(BigDecimal.valueOf(8), "EUR"),
            List.of(), List.of(), List.of(), List.of(), Instant.now(), context, false, false);
    }

    @Test
    void putThenGetReturnsTheSameQuote() {
        adapter.put(productId, "ctx-1", sampleQuote());

        Optional<PriceQuote> cached = adapter.get(productId, "ctx-1");

        assertThat(cached).isPresent();
        assertThat(cached.get().finalPrice().amount()).isEqualByComparingTo(BigDecimal.valueOf(8));
    }

    @Test
    void getReturnsEmptyWhenNothingIsCached() {
        assertThat(adapter.get(productId, "missing")).isEmpty();
    }

    @Test
    void evictAllForProductRemovesEveryEntryForThatProduct() {
        adapter.put(productId, "ctx-1", sampleQuote());
        adapter.put(productId, "ctx-2", sampleQuote());

        adapter.evictAllForProduct(productId);

        assertThat(adapter.get(productId, "ctx-1")).isEmpty();
        assertThat(adapter.get(productId, "ctx-2")).isEmpty();
    }
}
