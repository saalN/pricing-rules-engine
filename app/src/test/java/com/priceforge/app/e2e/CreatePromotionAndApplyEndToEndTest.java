package com.priceforge.app.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.priceforge.app.security.Role;
import com.priceforge.app.security.dto.LoginRequest;
import com.priceforge.app.security.dto.LoginResponse;
import com.priceforge.catalog.infrastructure.in.http.dto.CreateProductRequest;
import com.priceforge.catalog.infrastructure.in.http.dto.CreateProductResponse;
import com.priceforge.pricing.domain.model.CustomerSegment;
import com.priceforge.pricing.infrastructure.in.http.dto.CalculatePriceRequest;
import com.priceforge.pricing.infrastructure.in.http.dto.PriceQuoteResponse;
import com.priceforge.promotions.domain.model.DiscountType;
import com.priceforge.promotions.infrastructure.in.http.dto.CreatePromotionRequest;
import com.priceforge.promotions.infrastructure.in.http.dto.CreatePromotionResponse;
import com.priceforge.sharedkernel.CombinationMode;
import com.redis.testcontainers.RedisContainer;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * Exercises UC-10 (create promotion) and UC-11 (apply promotion during UC-06's price
 * calculation) together through the real stack — HTTP, JWT/RBAC, Postgres — proving a promotion
 * created by Marketing actually reduces the final price on top of the base price (no pricing
 * rules involved here, isolating UC-11's own discount application), and that write access to
 * promotions is restricted to ADMIN/MARKETING.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CreatePromotionAndApplyEndToEndTest {

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
    void appliesAPromotionCreatedByMarketingDuringAPriceCalculation() {
        String adminToken = login("admin@priceforge.com", Role.ADMIN);
        String marketingToken = login("marketing@priceforge.com", Role.MARKETING);

        CreateProductResponse product = restTemplate.postForObject(
            "/products",
            authenticated(adminToken, new CreateProductRequest(
                "SKU-E2E-PROMO-1", "Gadget", "A gadget", BigDecimal.valueOf(100), "EUR", "URBAN", null)),
            CreateProductResponse.class);

        CreatePromotionRequest promotionRequest = new CreatePromotionRequest(
            "Launch promo", "10 EUR off urban products",
            Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS),
            1000, 1, null, "URBAN", null, DiscountType.FIXED_AMOUNT, BigDecimal.TEN, "EUR", 10,
            CombinationMode.STACKABLE);
        ResponseEntity<CreatePromotionResponse> promotionResponse = restTemplate.postForEntity(
            "/promotions", authenticated(marketingToken, promotionRequest), CreatePromotionResponse.class);
        assertThat(promotionResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        CalculatePriceRequest calculateRequest = new CalculatePriceRequest(
            product.productId(), UUID.randomUUID(), CustomerSegment.VIP, 1, null, "URBAN", "EUR", null);
        ResponseEntity<PriceQuoteResponse> quoteResponse = restTemplate.postForEntity(
            "/price-calculations", authenticated(adminToken, calculateRequest), PriceQuoteResponse.class);

        assertThat(quoteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(quoteResponse.getBody().finalPriceAmount()).isEqualByComparingTo(BigDecimal.valueOf(90));
        assertThat(quoteResponse.getBody().appliedPromotions()).hasSize(1);
    }

    @Test
    void auditorCannotCreateAPromotion() {
        String auditorToken = login("auditor@priceforge.com", Role.AUDITOR);

        CreatePromotionRequest promotionRequest = new CreatePromotionRequest(
            "Unauthorized promo", "d", Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS),
            1000, 1, null, "URBAN", null, DiscountType.PERCENTAGE, BigDecimal.TEN, "EUR", 10,
            CombinationMode.STACKABLE);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/promotions", authenticated(auditorToken, promotionRequest), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
