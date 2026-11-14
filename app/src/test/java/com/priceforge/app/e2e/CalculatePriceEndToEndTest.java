package com.priceforge.app.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.priceforge.app.security.Role;
import com.priceforge.app.security.dto.LoginRequest;
import com.priceforge.app.security.dto.LoginResponse;
import com.priceforge.catalog.infrastructure.in.http.dto.CreateProductRequest;
import com.priceforge.catalog.infrastructure.in.http.dto.CreateProductResponse;
import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.sharedkernel.CustomerSegment;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.PricingRuleType;
import com.priceforge.pricing.infrastructure.in.http.dto.CalculatePriceRequest;
import com.priceforge.pricing.infrastructure.in.http.dto.CreatePricingRuleResponse;
import com.priceforge.pricing.infrastructure.in.http.dto.PriceQuoteResponse;
import com.priceforge.pricing.infrastructure.in.http.dto.PricingRuleDefinitionRequest;
import com.redis.testcontainers.RedisContainer;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises UC-05 (create rule) and UC-06 (calculate price) together through the real stack —
 * HTTP, JWT/RBAC, Postgres and Redis — proving a SEGMENT_BASED discount actually applies and that
 * write access to pricing rules is restricted to ADMIN/REVENUE_MANAGER.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CalculatePriceEndToEndTest {

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

    @Test
    void calculatesAPriceWithASegmentBasedDiscountAppliedThroughTheRealStack() {
        String adminToken = login("admin@priceforge.com", Role.ADMIN);

        CreateProductResponse product = restTemplate.postForObject(
            "/products",
            authenticated(adminToken, new CreateProductRequest(
                "SKU-E2E-PRICING-1", "Widget", "A widget", BigDecimal.valueOf(100), "EUR", "URBAN", null)),
            CreateProductResponse.class);

        PricingRuleDefinitionRequest ruleRequest = new PricingRuleDefinitionRequest(
            PricingRuleType.SEGMENT_BASED, null, null, CustomerSegment.VIP, null, null,
            DiscountType.PERCENTAGE, BigDecimal.valueOf(20), 10, CombinationMode.STACKABLE, null, null);
        ResponseEntity<CreatePricingRuleResponse> ruleResponse = restTemplate.postForEntity(
            "/pricing-rules", authenticated(adminToken, ruleRequest), CreatePricingRuleResponse.class);
        assertThat(ruleResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        CalculatePriceRequest calculateRequest = new CalculatePriceRequest(
            product.productId(), UUID.randomUUID(), CustomerSegment.VIP, 1, null, "URBAN", "EUR", null);
        ResponseEntity<PriceQuoteResponse> quoteResponse = restTemplate.postForEntity(
            "/price-calculations", authenticated(adminToken, calculateRequest), PriceQuoteResponse.class);

        assertThat(quoteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(quoteResponse.getBody().finalPriceAmount()).isEqualByComparingTo(BigDecimal.valueOf(80));
        assertThat(quoteResponse.getBody().appliedRules()).hasSize(1);
    }

    @Test
    void marketingCannotCreateAPricingRule() {
        String marketingToken = login("marketing@priceforge.com", Role.MARKETING);

        PricingRuleDefinitionRequest ruleRequest = new PricingRuleDefinitionRequest(
            PricingRuleType.SEGMENT_BASED, null, null, CustomerSegment.VIP, null, null,
            DiscountType.PERCENTAGE, BigDecimal.valueOf(10), 10, CombinationMode.STACKABLE, null, null);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/pricing-rules", authenticated(marketingToken, ruleRequest), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
