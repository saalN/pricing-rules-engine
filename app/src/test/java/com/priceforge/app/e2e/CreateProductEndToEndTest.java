package com.priceforge.app.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.priceforge.app.security.Role;
import com.priceforge.app.security.dto.LoginRequest;
import com.priceforge.app.security.dto.LoginResponse;
import com.priceforge.catalog.infrastructure.in.http.dto.CreateProductRequest;
import com.priceforge.catalog.infrastructure.in.http.dto.CreateProductResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises UC-01 through the real stack — HTTP, JWT authentication, @PreAuthorize RBAC
 * enforcement and Postgres persistence — rather than any single layer in isolation.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CreateProductEndToEndTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void adminCanCreateAProduct() {
        HttpEntity<CreateProductRequest> request = authenticatedRequest(
            login("admin@priceforge.com", Role.ADMIN),
            new CreateProductRequest("SKU-E2E-1", "Widget", "A widget", BigDecimal.TEN, "EUR", "URBAN", null));

        ResponseEntity<CreateProductResponse> response =
            restTemplate.postForEntity("/products", request, CreateProductResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().productId()).isNotNull();
    }

    @Test
    void auditorCannotCreateAProduct() {
        HttpEntity<CreateProductRequest> request = authenticatedRequest(
            login("auditor@priceforge.com", Role.AUDITOR),
            new CreateProductRequest("SKU-E2E-2", "Widget", "A widget", BigDecimal.TEN, "EUR", "URBAN", null));

        ResponseEntity<String> response = restTemplate.postForEntity("/products", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void requestWithoutATokenIsRejected() {
        HttpEntity<CreateProductRequest> request = new HttpEntity<>(
            new CreateProductRequest("SKU-E2E-3", "Widget", "A widget", BigDecimal.TEN, "EUR", "URBAN", null));

        ResponseEntity<String> response = restTemplate.postForEntity("/products", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String login(String username, Role role) {
        LoginResponse response =
            restTemplate.postForObject("/auth/token", new LoginRequest(username, role), LoginResponse.class);
        return response.accessToken();
    }

    private <T> HttpEntity<T> authenticatedRequest(String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }
}
