package com.priceforge.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.priceforge.catalog.infrastructure.in.http.dto.CreateProductRequest;
import com.redis.testcontainers.RedisContainer;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** The real application started with the {@code prod} profile and a proper JWT secret. */
@Testcontainers
@ActiveProfiles("prod")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "JWT_SECRET=a-long-random-secret-that-is-not-the-dev-one-0123456789")
class ProdProfileEndToEndTest {

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
    private ApplicationContext context;

    @Test
    void theGuardRanAndTheDemoTokenEndpointDoesNotExist() {
        assertThat(context.getBeansOfType(ProductionSecurityGuard.class)).hasSize(1);
        assertThat(context.getBeansOfType(AuthController.class)).isEmpty();

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/auth/token", "{\"username\":\"attacker\",\"role\":\"ADMIN\"}", String.class);

        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).doesNotContain("accessToken");
    }

    @Test
    void swaggerUiAndTheApiDocsAreNotExposed() {
        assertThat(restTemplate.getForEntity("/swagger-ui/index.html", String.class).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.getForEntity("/v3/api-docs", String.class).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void aForgedTokenSignedWithTheDevelopmentSecretIsRejected() {
        String forged = io.jsonwebtoken.Jwts.builder()
            .subject("attacker")
            .claim("role", "ADMIN")
            .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                "priceforge-local-dev-only-hmac-secret-change-me-32b+".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            .compact();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(forged);

        ResponseEntity<String> response = restTemplate.exchange(
            "/products", org.springframework.http.HttpMethod.POST,
            new org.springframework.http.HttpEntity<>(
                new CreateProductRequest("SKU-FORGED", "x", "d", BigDecimal.TEN, "EUR", "URBAN", null), headers),
            String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
