package com.priceforge.app.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceforge.app.security.Role;
import com.priceforge.app.security.dto.LoginRequest;
import com.priceforge.app.security.dto.LoginResponse;
import com.priceforge.catalog.infrastructure.in.http.dto.CreateProductRequest;
import com.priceforge.catalog.infrastructure.in.http.dto.CreateProductResponse;
import com.priceforge.catalog.infrastructure.in.http.dto.UpdateProductBasePriceRequest;
import com.priceforge.pricing.domain.model.CustomerSegment;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.PricingRuleType;
import com.priceforge.pricing.infrastructure.in.http.dto.CalculatePriceRequest;
import com.priceforge.pricing.infrastructure.in.http.dto.CreatePricingRuleResponse;
import com.priceforge.pricing.infrastructure.in.http.dto.PriceQuoteResponse;
import com.priceforge.pricing.infrastructure.in.http.dto.PricingRuleDefinitionRequest;
import com.priceforge.sharedkernel.CombinationMode;
import com.redis.testcontainers.RedisContainer;
import java.math.BigDecimal;
import java.time.Duration;
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
 * UC-14/UC-15 through the real stack: domain events written to each context's outbox, relayed
 * to Audit, persisted, then queried, replayed and exported over HTTP with real JWTs.
 */
@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "priceforge.outbox.relay-interval-ms=200")
class AuditTrailEndToEndTest {

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

    @Test
    void everyContextsEventsReachTheAuditTrailAndCanBeQueriedReplayedAndExported() throws Exception {
        String adminToken = login("admin@priceforge.com", Role.ADMIN);
        String auditorToken = login("auditor@priceforge.com", Role.AUDITOR);

        CreateProductResponse product = restTemplate.postForObject(
            "/products",
            authenticated(adminToken, new CreateProductRequest(
                "SKU-E2E-AUDIT-1", "Widget", "A widget", BigDecimal.valueOf(100), "EUR", "URBAN", null)),
            CreateProductResponse.class);
        UUID productId = product.productId();

        CreatePricingRuleResponse rule = restTemplate.postForObject(
            "/pricing-rules",
            authenticated(adminToken, new PricingRuleDefinitionRequest(
                PricingRuleType.SEGMENT_BASED, null, null, CustomerSegment.VIP, null, null,
                DiscountType.PERCENTAGE, BigDecimal.valueOf(20), 10, CombinationMode.STACKABLE, null, null)),
            CreatePricingRuleResponse.class);

        UUID userId = UUID.randomUUID();
        ResponseEntity<PriceQuoteResponse> quote = restTemplate.postForEntity(
            "/price-calculations",
            authenticated(adminToken, new CalculatePriceRequest(
                productId, userId, CustomerSegment.VIP, 2, null, "URBAN", "EUR", null)),
            PriceQuoteResponse.class);
        assertThat(quote.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(quote.getBody().finalPriceAmount()).isEqualByComparingTo("80");

        // UC-14: the calculation, with its full contextSnapshot, queryable by product and user.
        JsonNode page = awaitJson(
            auditorToken, "/audit/price-calculations?productId=" + productId + "&userId=" + userId, 1);
        JsonNode calculation = page.get("items").get(0);
        assertThat(calculation.get("userId").asText()).isEqualTo(userId.toString());
        assertThat(calculation.get("payload").get("contextSnapshot").get("quantity").asInt()).isEqualTo(2);
        assertThat(calculation.get("payload").get("contextSnapshot").get("userSegment").asText()).isEqualTo("VIP");
        String calculationId = calculation.get("eventId").asText();

        // ...and replaying it against the unchanged rules gives the same price.
        JsonNode sameReplay = getJson(auditorToken, "/audit/price-calculations/" + calculationId + "/replay");
        assertThat(sameReplay.get("matches").asBoolean()).isTrue();
        assertThat(sameReplay.get("replayedFinalPriceAmount").decimalValue()).isEqualByComparingTo("80");

        // After the product's base price changes, the same replay detects the difference.
        ResponseEntity<String> priceChange = restTemplate.exchange(
            "/products/" + productId + "/base-price", HttpMethod.PATCH,
            authenticated(adminToken, new UpdateProductBasePriceRequest(BigDecimal.valueOf(150))), String.class);
        assertThat(priceChange.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode changedReplay = getJson(auditorToken, "/audit/price-calculations/" + calculationId + "/replay");
        assertThat(changedReplay.get("matches").asBoolean()).isFalse();
        assertThat(changedReplay.get("originalFinalPriceAmount").decimalValue()).isEqualByComparingTo("80");
        assertThat(changedReplay.get("replayedFinalPriceAmount").decimalValue()).isEqualByComparingTo("120");

        // UC-15: who changed what, before and after.
        JsonNode productHistory = awaitHistory(auditorToken, "PRODUCT", productId.toString(), 2);
        assertThat(productHistory.get(0).get("eventType").asText()).isEqualTo("ProductCreated");
        JsonNode priceUpdate = productHistory.get(1);
        assertThat(priceUpdate.get("eventType").asText()).isEqualTo("ProductPriceBaseUpdated");
        assertThat(priceUpdate.get("changedBy").asText()).isEqualTo("admin@priceforge.com");
        assertThat(priceUpdate.get("previousState").asText()).isEqualTo("100.0000 EUR");
        assertThat(priceUpdate.get("newState").asText()).isEqualTo("150 EUR");

        JsonNode ruleHistory = awaitHistory(auditorToken, "PRICING_RULE", rule.ruleId().toString(), 1);
        assertThat(ruleHistory.get(0).get("eventType").asText()).isEqualTo("PricingRuleCreated");

        // Compliance export, both formats.
        ResponseEntity<String> csv = restTemplate.exchange(
            "/audit/export?format=CSV&entityType=PRODUCT&entityId=" + productId, HttpMethod.GET,
            authenticated(auditorToken, null), String.class);
        assertThat(csv.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(csv.getHeaders().getContentType().toString()).startsWith("text/csv");
        assertThat(csv.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("audit-trail.csv");
        assertThat(csv.getBody()).startsWith("eventId,eventType,").contains("ProductCreated", "ProductPriceBaseUpdated");

        JsonNode json = getJson(auditorToken, "/audit/export?format=JSON&entityType=PRODUCT&entityId=" + productId);
        assertThat(json.isArray()).isTrue();
        assertThat(json).hasSize(2);
        assertThat(json.get(0).get("payload").get("aggregateId").asText()).isEqualTo(productId.toString());
    }

    @Test
    void auditReadAccessFollowsTheRolesTable() {
        String adminToken = login("admin@priceforge.com", Role.ADMIN);
        String revenueManagerToken = login("revenue@priceforge.com", Role.REVENUE_MANAGER);
        String marketingToken = login("marketing@priceforge.com", Role.MARKETING);
        String auditorToken = login("auditor@priceforge.com", Role.AUDITOR);

        // Every role can read the histories...
        for (String token : new String[] {adminToken, revenueManagerToken, marketingToken, auditorToken}) {
            assertThat(status(token, "/audit/price-calculations")).isEqualTo(HttpStatus.OK);
            assertThat(status(token, "/audit/history/PROMOTION/" + UUID.randomUUID())).isEqualTo(HttpStatus.OK);
        }
        // ...but only the Admin and the Auditor may export or replay.
        assertThat(status(adminToken, "/audit/export?format=JSON")).isEqualTo(HttpStatus.OK);
        assertThat(status(auditorToken, "/audit/export?format=JSON")).isEqualTo(HttpStatus.OK);
        assertThat(status(revenueManagerToken, "/audit/export?format=JSON")).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(status(marketingToken, "/audit/export?format=CSV")).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(status(marketingToken, "/audit/price-calculations/" + UUID.randomUUID() + "/replay"))
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(status(auditorToken, "/audit/price-calculations/" + UUID.randomUUID() + "/replay"))
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void auditEndpointsRequireAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity("/audit/price-calculations", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void theAuditorCannotCalculateAPriceBecauseThatWouldWrite() {
        String auditorToken = login("auditor@priceforge.com", Role.AUDITOR);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/price-calculations",
            authenticated(auditorToken, new CalculatePriceRequest(
                UUID.randomUUID(), UUID.randomUUID(), CustomerSegment.VIP, 1, null, "URBAN", "EUR", null)),
            String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private JsonNode awaitJson(String token, String path, int expectedTotal) {
        return await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(200))
            .until(() -> getJson(token, path), body -> body.get("totalElements").asInt() >= expectedTotal);
    }

    private JsonNode awaitHistory(String token, String entityType, String entityId, int expectedSize) {
        return await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(200))
            .until(() -> getJson(token, "/audit/history/" + entityType + "/" + entityId),
                body -> body.size() >= expectedSize);
    }

    private JsonNode getJson(String token, String path) throws Exception {
        ResponseEntity<String> response =
            restTemplate.exchange(path, HttpMethod.GET, authenticated(token, null), String.class);
        assertThat(response.getStatusCode()).as(path).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody());
    }

    private HttpStatus status(String token, String path) {
        return HttpStatus.valueOf(
            restTemplate.exchange(path, HttpMethod.GET, authenticated(token, null), String.class)
                .getStatusCode().value());
    }

    private String login(String username, Role role) {
        LoginResponse response =
            restTemplate.postForObject("/auth/token", new LoginRequest(username, role), LoginResponse.class);
        return response.accessToken();
    }

    private <T> HttpEntity<T> authenticated(String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }
}
