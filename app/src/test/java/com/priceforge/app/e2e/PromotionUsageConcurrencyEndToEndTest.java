package com.priceforge.app.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceforge.app.security.Role;
import com.priceforge.app.security.dto.LoginRequest;
import com.priceforge.app.security.dto.LoginResponse;
import com.priceforge.catalog.infrastructure.in.http.dto.CreateProductRequest;
import com.priceforge.catalog.infrastructure.in.http.dto.CreateProductResponse;
import com.priceforge.pricing.infrastructure.in.http.dto.CalculatePriceRequest;
import com.priceforge.pricing.infrastructure.in.http.dto.PriceQuoteResponse;
import com.priceforge.promotions.domain.model.DiscountType;
import com.priceforge.promotions.infrastructure.in.http.dto.CreatePromotionRequest;
import com.priceforge.promotions.infrastructure.in.http.dto.CreatePromotionResponse;
import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.sharedkernel.CustomerSegment;
import com.redis.testcontainers.RedisContainer;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * UC-11's usage limits under real contention: many simultaneous price calculations through HTTP,
 * the connection pool and Postgres row locks. The adapter-level test only races two callers; this
 * is the same guarantee at a scale where a lost update or a lock-ordering deadlock would show up.
 */
@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "priceforge.outbox.relay-interval-ms=200")
class PromotionUsageConcurrencyEndToEndTest {

    private static final int THREADS = 32;

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static final RedisContainer REDIS = new RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME);

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void theGlobalLimitIsNeverExceededWhenManyUsersApplyAtOnce() throws Exception {
        String adminToken = token("admin@priceforge.com", Role.ADMIN);
        String marketingToken = token("marketing@priceforge.com", Role.MARKETING);
        UUID productId = createProduct(adminToken, "SKU-CONC-GLOBAL", "CONC-GLOBAL");
        UUID promotion = createPromotion(marketingToken, "Global limit", "CONC-GLOBAL", 20, 25, 100);

        List<PriceQuoteResponse> quotes = calculateConcurrently(
            adminToken, productId, 100, () -> UUID.randomUUID());

        assertThat(quotes).hasSize(100);
        assertThat(quotes.stream().filter(quote -> !quote.appliedPromotions().isEmpty()).count()).isEqualTo(25);
        assertThat(quotes.stream().filter(quote -> quote.finalPriceAmount().compareTo(BigDecimal.valueOf(80)) == 0)
            .count()).isEqualTo(25);

        assertThat(globalUsageCount(promotion)).isEqualTo(25);
        assertThat(sumOfPerUserUsage(promotion)).isEqualTo(25);
        assertThat(status(promotion)).isEqualTo("EXHAUSTED");

        // The trail agrees: exactly 25 applications and a single exhaustion event.
        JsonNode history = Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(300))
            .until(() -> history(token("auditor@priceforge.com", Role.AUDITOR), promotion),
                entries -> count(entries, "PromotionApplied") >= 25 && count(entries, "PromotionExhausted") >= 1);
        assertThat(count(history, "PromotionApplied")).isEqualTo(25);
        assertThat(count(history, "PromotionExhausted")).isEqualTo(1);
    }

    @Test
    void thePerUserLimitHoldsWhenOneUserFloodsTheEndpoint() throws Exception {
        String adminToken = token("admin@priceforge.com", Role.ADMIN);
        String marketingToken = token("marketing@priceforge.com", Role.MARKETING);
        UUID productId = createProduct(adminToken, "SKU-CONC-USER", "CONC-USER");
        UUID promotion = createPromotion(marketingToken, "Per user limit", "CONC-USER", 20, 1000, 3);
        UUID flooder = UUID.randomUUID();

        List<PriceQuoteResponse> quotes = calculateConcurrently(adminToken, productId, 60, () -> flooder);

        assertThat(quotes).hasSize(60);
        assertThat(quotes.stream().filter(quote -> !quote.appliedPromotions().isEmpty()).count()).isEqualTo(3);
        assertThat(globalUsageCount(promotion)).isEqualTo(3);
        assertThat(sumOfPerUserUsage(promotion)).isEqualTo(3);
        assertThat(status(promotion)).isEqualTo("ACTIVE");
    }

    @Test
    void severalPromotionsLockedInThePriorityOrderNeverDeadlock() throws Exception {
        String adminToken = token("admin@priceforge.com", Role.ADMIN);
        String marketingToken = token("marketing@priceforge.com", Role.MARKETING);
        UUID productId = createProduct(adminToken, "SKU-CONC-MULTI", "CONC-MULTI");
        UUID high = createPromotion(marketingToken, "High priority", "CONC-MULTI", 10, 40, 100, 30);
        UUID middle = createPromotion(marketingToken, "Middle priority", "CONC-MULTI", 5, 40, 100, 20);
        UUID low = createPromotion(marketingToken, "Low priority", "CONC-MULTI", 2, 40, 100, 10);

        // calculateConcurrently asserts every response is a 200, so a deadlock victim (a 500) fails here.
        List<PriceQuoteResponse> quotes = calculateConcurrently(adminToken, productId, 120, () -> UUID.randomUUID());

        assertThat(quotes).hasSize(120);
        for (UUID promotion : List.of(high, middle, low)) {
            assertThat(globalUsageCount(promotion)).as("usage of " + promotion).isEqualTo(40);
            assertThat(sumOfPerUserUsage(promotion)).isEqualTo(40);
            assertThat(status(promotion)).isEqualTo("EXHAUSTED");
        }
    }

    private List<PriceQuoteResponse> calculateConcurrently(
            String token, UUID productId, int requests, Callable<UUID> userSupplier) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<ResponseEntity<PriceQuoteResponse>>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < requests; i++) {
                UUID userId = userSupplier.call();
                futures.add(executor.submit(() -> {
                    go.await();
                    return restTemplate.postForEntity(
                        "/price-calculations",
                        authenticated(token, new CalculatePriceRequest(
                            productId, userId, CustomerSegment.STANDARD, 1, null, "URBAN", "EUR", null)),
                        PriceQuoteResponse.class);
                }));
            }
            go.countDown();

            List<PriceQuoteResponse> quotes = new ArrayList<>();
            for (Future<ResponseEntity<PriceQuoteResponse>> future : futures) {
                ResponseEntity<PriceQuoteResponse> response = future.get(120, TimeUnit.SECONDS);
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                quotes.add(response.getBody());
            }
            return quotes;
        } finally {
            executor.shutdownNow();
        }
    }

    private UUID createProduct(String adminToken, String sku, String category) {
        return restTemplate.postForObject(
            "/products",
            authenticated(adminToken, new CreateProductRequest(
                sku, "Product", "d", BigDecimal.valueOf(100), "EUR", category, null)),
            CreateProductResponse.class).productId();
    }

    private UUID createPromotion(
            String marketingToken, String name, String category, int discountAmount, int globalLimit, int perUserLimit) {
        return createPromotion(marketingToken, name, category, discountAmount, globalLimit, perUserLimit, 10);
    }

    private UUID createPromotion(
            String marketingToken, String name, String category, int discountAmount, int globalLimit,
            int perUserLimit, int priority) {
        ResponseEntity<CreatePromotionResponse> response = restTemplate.postForEntity(
            "/promotions",
            authenticated(marketingToken, new CreatePromotionRequest(
                name, "d", Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS),
                globalLimit, perUserLimit, null, category, null, DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(discountAmount), "EUR", priority, CombinationMode.STACKABLE)),
            CreatePromotionResponse.class);
        assertThat(response.getStatusCode()).as("creating " + name).isEqualTo(HttpStatus.CREATED);
        return response.getBody().promotionId();
    }

    private int globalUsageCount(UUID promotion) {
        return jdbcTemplate.queryForObject(
            "SELECT global_usage_count FROM promotions.promotion WHERE id = ?", Integer.class, promotion);
    }

    private int sumOfPerUserUsage(UUID promotion) {
        return jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(usage_count), 0) FROM promotions.promotion_usage WHERE promotion_id = ?",
            Integer.class, promotion);
    }

    private String status(UUID promotion) {
        return jdbcTemplate.queryForObject(
            "SELECT status FROM promotions.promotion WHERE id = ?", String.class, promotion);
    }

    private JsonNode history(String auditorToken, UUID promotion) throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
            "/audit/history/PROMOTION/" + promotion, HttpMethod.GET, authenticated(auditorToken, null), String.class);
        return objectMapper.readTree(response.getBody());
    }

    private static long count(JsonNode entries, String eventType) {
        long total = 0;
        for (JsonNode entry : entries) {
            if (entry.path("eventType").asText().equals(eventType)) {
                total++;
            }
        }
        return total;
    }

    private String token(String username, Role role) {
        return restTemplate.postForObject("/auth/token", new LoginRequest(username, role), LoginResponse.class)
            .accessToken();
    }

    private <T> HttpEntity<T> authenticated(String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }
}
