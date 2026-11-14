package com.priceforge.app.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceforge.app.security.Role;
import com.priceforge.app.security.dto.LoginRequest;
import com.priceforge.app.security.dto.LoginResponse;
import com.priceforge.catalog.infrastructure.in.http.dto.CreateProductRequest;
import com.priceforge.catalog.infrastructure.in.http.dto.CreateProductResponse;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.PricingRuleType;
import com.priceforge.pricing.infrastructure.in.http.dto.CalculatePriceRequest;
import com.priceforge.pricing.infrastructure.in.http.dto.ExcludedPromotionResponse;
import com.priceforge.pricing.infrastructure.in.http.dto.ExcludedRuleResponse;
import com.priceforge.pricing.infrastructure.in.http.dto.PriceQuoteResponse;
import com.priceforge.pricing.infrastructure.in.http.dto.PricingRuleDefinitionRequest;
import com.priceforge.promotions.infrastructure.in.http.dto.CreatePromotionRequest;
import com.priceforge.promotions.infrastructure.in.http.dto.CreatePromotionResponse;
import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.sharedkernel.CustomerSegment;
import com.redis.testcontainers.RedisContainer;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Cross-context regression: one calculation that crosses Catalog (base price), Pricing (rules with
 * conflicts), Promotions (campaigns with conflicts and usage limits) and Audit (the trail it
 * leaves), through the real stack. UC-06, UC-07, UC-11 and UC-13 together; each scenario scopes its
 * rules and promotions to its own category so they cannot leak into each other.
 */
@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "priceforge.outbox.relay-interval-ms=200")
class PricingAndPromotionsCombinedEndToEndTest {

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
    private ObjectMapper objectMapper;

    private String adminToken;
    private String marketingToken;
    private String auditorToken;

    @Test
    void conflictsOnBothLayersResolveToTheExpectedPriceAndLeaveAnAuditTrail() throws Exception {
        login();
        String category = "COMBO-A";
        UUID productId = createProduct("SKU-COMBO-A", 200, category);

        // Pricing layer: Ra and Rb tie at priority 30 and are both EXCLUSIVE, so the earlier-created
        // Ra wins and Rb is a genuine conflict (UC-07); Rc, lower priority, is simply outranked.
        createRule(10, DiscountType.PERCENTAGE, 30, CombinationMode.EXCLUSIVE, category);
        UUID tiebreakLoser = createRule(5, DiscountType.FIXED_AMOUNT, 30, CombinationMode.EXCLUSIVE, category);
        UUID outranked = createRule(50, DiscountType.PERCENTAGE, 10, CombinationMode.STACKABLE, category);

        // Promotions layer: P1 and P2 tie at 20 and both stack (each computed on the same starting
        // price); P3 and P4 tie at 10 and are both EXCLUSIVE, so P3 wins; P5 is outranked by P3.
        UUID p1 = createPromotion("P1", 20, DiscountType.FIXED_AMOUNT, 20, CombinationMode.STACKABLE, category, 1000, 10);
        UUID p2 = createPromotion("P2", 10, DiscountType.PERCENTAGE, 20, CombinationMode.STACKABLE, category, 1000, 10);
        UUID p3 = createPromotion("P3", 5, DiscountType.FIXED_AMOUNT, 10, CombinationMode.EXCLUSIVE, category, 1000, 10);
        UUID p4 = createPromotion("P4", 7, DiscountType.FIXED_AMOUNT, 10, CombinationMode.EXCLUSIVE, category, 1000, 10);
        UUID p5 = createPromotion("P5", 3, DiscountType.FIXED_AMOUNT, 5, CombinationMode.STACKABLE, category, 1000, 10);

        UUID userId = UUID.randomUUID();
        PriceQuoteResponse quote = calculate(productId, userId);

        // 200 - 10% (Ra) = 180; promotions on 180: P1 20 + P2 10% (18) = 38 -> 142; P3 5 -> 137.
        assertThat(quote.basePriceAmount()).isEqualByComparingTo("200");
        assertThat(quote.finalPriceAmount()).isEqualByComparingTo("137");

        assertThat(quote.appliedRules()).hasSize(1);
        assertThat(quote.appliedRules().get(0).discountApplied()).isEqualByComparingTo("20");
        assertThat(quote.excludedRules())
            .extracting(ExcludedRuleResponse::ruleId, ExcludedRuleResponse::reason)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(tiebreakLoser, "EXCLUDED_BY_TIEBREAK"),
                org.assertj.core.groups.Tuple.tuple(outranked, "EXCLUDED_BY_HIGHER_PRIORITY"));

        assertThat(quote.appliedPromotions())
            .extracting(applied -> applied.sourceId(), applied -> applied.discountApplied().setScale(0))
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(p1.toString(), BigDecimal.valueOf(20)),
                org.assertj.core.groups.Tuple.tuple(p2.toString(), BigDecimal.valueOf(18)),
                org.assertj.core.groups.Tuple.tuple(p3.toString(), BigDecimal.valueOf(5)));
        assertThat(quote.excludedPromotions())
            .extracting(ExcludedPromotionResponse::sourceId, ExcludedPromotionResponse::reason)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(p4.toString(), "EXCLUDED_BY_TIEBREAK"),
                org.assertj.core.groups.Tuple.tuple(p5.toString(), "EXCLUDED_BY_HIGHER_PRIORITY"));

        // A second identical request is served from the pricing cache but must still re-run the
        // promotions (their usage limits are never cached) and reach the same price.
        assertThat(calculate(productId, userId).finalPriceAmount()).isEqualByComparingTo("137");

        // Audit: the calculation, its pricing conflict, and each applied promotion.
        JsonNode calculations = await(() -> getJson("/audit/price-calculations?productId=" + productId),
            body -> body.get("totalElements").asInt() >= 2);
        assertThat(calculations.get("items").get(0).get("payload").get("finalPriceAmount").decimalValue())
            .isEqualByComparingTo("137");

        JsonNode productTrail = await(
            () -> getJson("/audit/export?format=JSON&productId=" + productId),
            body -> containsEventType(body, "RuleConflictDetected"));
        JsonNode conflict = findByEventType(productTrail, "RuleConflictDetected");
        assertThat(conflict.get("reason").asText()).isEqualTo("TIEBREAK");
        assertThat(conflict.get("newState").asText()).contains(tiebreakLoser.toString());

        for (UUID promotion : List.of(p1, p2, p3)) {
            JsonNode history = await(() -> getJson("/audit/history/PROMOTION/" + promotion),
                body -> containsEventType(body, "PromotionApplied"));
            assertThat(findByEventType(history, "PromotionApplied").get("userId").asText())
                .isEqualTo(userId.toString());
        }

        // Replaying re-runs the pricing rules only, so it reports 180 against the stored 137: the
        // documented difference when promotions were part of the original calculation.
        String firstCalculation = calculations.get("items").get(0).get("eventId").asText();
        JsonNode replay = getJson("/audit/price-calculations/" + firstCalculation + "/replay");
        assertThat(replay.get("replayedFinalPriceAmount").decimalValue()).isEqualByComparingTo("180");
        assertThat(replay.get("matches").asBoolean()).isFalse();
    }

    @Test
    void aPromotionThatCouldDriveThePriceToZeroIsRejectedAndUsageLimitsAreEnforcedPerUserAndGlobally()
            throws Exception {
        login();
        String category = "COMBO-B";
        UUID productId = createProduct("SKU-COMBO-B", 100, category);
        createRule(60, DiscountType.PERCENTAGE, 10, CombinationMode.STACKABLE, category);

        // UC-13: worst case is the 60% rule plus the promotion; 60 + 50 would reach 110 on a 100 product.
        ResponseEntity<String> rejected = restTemplate.postForEntity(
            "/promotions",
            authenticated(marketingToken, promotionRequest(
                "Too generous", 50, DiscountType.FIXED_AMOUNT, 10, CombinationMode.STACKABLE, category, 1000, 10)),
            String.class);
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // 60 + 30 leaves 10 in the worst case, so this one is accepted. Two uses globally, one per user.
        UUID promotion = createPromotion("Fair", 30, DiscountType.FIXED_AMOUNT, 10, CombinationMode.STACKABLE, category, 2, 1);

        UUID userOne = UUID.randomUUID();
        UUID userTwo = UUID.randomUUID();
        UUID userThree = UUID.randomUUID();

        // 100 - 60% = 40; the promotion takes 30 more -> 10.
        assertThat(calculate(productId, userOne).finalPriceAmount()).isEqualByComparingTo("10");

        // Same user again: per-user limit reached, so only the pricing rule applies.
        PriceQuoteResponse secondForUserOne = calculate(productId, userOne);
        assertThat(secondForUserOne.finalPriceAmount()).isEqualByComparingTo("40");
        assertThat(secondForUserOne.excludedPromotions())
            .extracting(ExcludedPromotionResponse::sourceId, ExcludedPromotionResponse::reason)
            .containsExactly(org.assertj.core.groups.Tuple.tuple(promotion.toString(), "LIMIT_REACHED"));

        // Another user takes the last global use...
        assertThat(calculate(productId, userTwo).finalPriceAmount()).isEqualByComparingTo("10");

        // ...so a third user no longer gets it, and the promotion is no longer even a candidate.
        PriceQuoteResponse forUserThree = calculate(productId, userThree);
        assertThat(forUserThree.finalPriceAmount()).isEqualByComparingTo("40");
        assertThat(forUserThree.appliedPromotions()).isEmpty();

        // Audit sees exactly two applications and the exhaustion.
        JsonNode history = await(() -> getJson("/audit/history/PROMOTION/" + promotion),
            body -> containsEventType(body, "PromotionExhausted"));
        long applications = 0;
        for (JsonNode entry : history) {
            if (entry.get("eventType").asText().equals("PromotionApplied")) {
                applications++;
            }
        }
        assertThat(applications).isEqualTo(2);
    }

    private void login() {
        adminToken = token("admin@priceforge.com", Role.ADMIN);
        marketingToken = token("marketing@priceforge.com", Role.MARKETING);
        auditorToken = token("auditor@priceforge.com", Role.AUDITOR);
    }

    private UUID createProduct(String sku, int basePrice, String category) {
        CreateProductResponse response = restTemplate.postForObject(
            "/products",
            authenticated(adminToken, new CreateProductRequest(
                sku, "Product " + sku, "d", BigDecimal.valueOf(basePrice), "EUR", category, null)),
            CreateProductResponse.class);
        return response.productId();
    }

    private UUID createRule(int discount, DiscountType type, int priority, CombinationMode mode, String category) {
        ResponseEntity<com.priceforge.pricing.infrastructure.in.http.dto.CreatePricingRuleResponse> response =
            restTemplate.postForEntity(
                "/pricing-rules",
                authenticated(adminToken, new PricingRuleDefinitionRequest(
                    PricingRuleType.SEGMENT_BASED, null, null, CustomerSegment.VIP, null, null,
                    type, BigDecimal.valueOf(discount), priority, mode, null, category)),
                com.priceforge.pricing.infrastructure.in.http.dto.CreatePricingRuleResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().ruleId();
    }

    private UUID createPromotion(
            String name, int discount, DiscountType type, int priority, CombinationMode mode, String category,
            int globalLimit, int perUserLimit) {
        ResponseEntity<CreatePromotionResponse> response = restTemplate.postForEntity(
            "/promotions",
            authenticated(marketingToken, promotionRequest(
                name, discount, type, priority, mode, category, globalLimit, perUserLimit)),
            CreatePromotionResponse.class);
        assertThat(response.getStatusCode()).as("creating promotion " + name).isEqualTo(HttpStatus.CREATED);
        return response.getBody().promotionId();
    }

    private CreatePromotionRequest promotionRequest(
            String name, int discount, DiscountType type, int priority, CombinationMode mode, String category,
            int globalLimit, int perUserLimit) {
        return new CreatePromotionRequest(
            name, "d", Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS),
            globalLimit, perUserLimit, null, category, null,
            com.priceforge.promotions.domain.model.DiscountType.valueOf(type.name()),
            BigDecimal.valueOf(discount), "EUR", priority, mode);
    }

    private PriceQuoteResponse calculate(UUID productId, UUID userId) {
        ResponseEntity<PriceQuoteResponse> response = restTemplate.postForEntity(
            "/price-calculations",
            authenticated(adminToken, new CalculatePriceRequest(
                productId, userId, CustomerSegment.VIP, 1, null, "URBAN", "EUR", null)),
            PriceQuoteResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private JsonNode await(java.util.concurrent.Callable<JsonNode> fetch, java.util.function.Predicate<JsonNode> ready) {
        return org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(200))
            .until(fetch, ready);
    }

    private static boolean containsEventType(JsonNode entries, String eventType) {
        return findByEventType(entries, eventType) != null;
    }

    private static JsonNode findByEventType(JsonNode entries, String eventType) {
        for (JsonNode entry : entries) {
            if (entry.path("eventType").asText().equals(eventType)) {
                return entry;
            }
        }
        return null;
    }

    private JsonNode getJson(String path) throws Exception {
        ResponseEntity<String> response =
            restTemplate.exchange(path, HttpMethod.GET, authenticated(auditorToken, null), String.class);
        assertThat(response.getStatusCode()).as(path).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody());
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
