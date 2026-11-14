package com.priceforge.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Keeps the published API contract in step with the functional spec: every endpoint's OpenAPI
 * summary must name the use case it implements, and the set of endpoints must be exactly the one
 * below, so adding or relabelling an endpoint is a deliberate change to this table. UC-07, UC-11,
 * UC-12 and UC-13 have no endpoint of their own: they run inside UC-06, UC-10 and the scheduler.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiDocumentationTest {

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

    private static final Map<String, String> EXPECTED_UC_PREFIX_BY_OPERATION = Map.ofEntries(
        Map.entry("POST /products", "UC-01:"),
        Map.entry("PATCH /products/{productId}/base-price", "UC-02:"),
        Map.entry("POST /products/{productId}/deactivate", "UC-03:"),
        Map.entry("PATCH /products/{productId}/category", "UC-04:"),
        Map.entry("POST /pricing-rules", "UC-05:"),
        Map.entry("POST /price-calculations", "UC-06:"),
        Map.entry("POST /price-calculations/simulate", "UC-08:"),
        Map.entry("PATCH /pricing-rules/{ruleId}", "UC-09:"),
        Map.entry("POST /pricing-rules/{ruleId}/deactivate", "UC-09:"),
        Map.entry("POST /promotions", "UC-10:"),
        Map.entry("GET /audit/price-calculations", "UC-14:"),
        Map.entry("GET /audit/price-calculations/{eventId}/replay", "UC-14:"),
        Map.entry("GET /audit/history/{entityType}/{entityId}", "UC-15:"),
        Map.entry("GET /audit/export", "UC-14/UC-15:"),
        Map.entry("POST /auth/token", "Demo utility:"));

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void everyEndpointIsDocumentedWithTheUseCaseItImplements() throws Exception {
        Map<String, String> summaries = new TreeMap<>();
        JsonNode paths = apiDocs().get("paths");
        paths.fields().forEachRemaining(path -> path.getValue().fields().forEachRemaining(operation ->
            summaries.put(operation.getKey().toUpperCase() + " " + path.getKey(),
                operation.getValue().path("summary").asText(""))));

        assertThat(summaries.keySet()).containsExactlyInAnyOrderElementsOf(EXPECTED_UC_PREFIX_BY_OPERATION.keySet());
        EXPECTED_UC_PREFIX_BY_OPERATION.forEach((operation, prefix) ->
            assertThat(summaries.get(operation)).as(operation).startsWith(prefix));
    }

    @Test
    void everyOperationBelongsToATagSoSwaggerUiGroupsThem() throws Exception {
        apiDocs().get("paths").fields().forEachRemaining(path -> path.getValue().fields().forEachRemaining(operation ->
            assertThat(operation.getValue().path("tags")).as(operation.getKey() + " " + path.getKey()).isNotEmpty()));
    }

    @Test
    void swaggerUiAndTheApiDocsAreReachableWithoutAToken() {
        ResponseEntity<String> ui = restTemplate.getForEntity("/swagger-ui/index.html", String.class);
        ResponseEntity<String> docs = restTemplate.getForEntity("/v3/api-docs", String.class);

        assertThat(ui.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ui.getBody()).contains("Swagger UI");
        assertThat(docs.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void theDocumentDeclaresTheBearerSchemeSoSwaggerUiCanAuthorise() throws Exception {
        JsonNode securitySchemes = apiDocs().path("components").path("securitySchemes");

        assertThat(securitySchemes).isNotEmpty();
        assertThat(securitySchemes.toString()).containsIgnoringCase("bearer");
    }

    private JsonNode apiDocs() throws Exception {
        return objectMapper.readTree(restTemplate.getForObject("/v3/api-docs", String.class));
    }
}
