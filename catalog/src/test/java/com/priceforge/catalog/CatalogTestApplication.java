package com.priceforge.catalog;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test-only bootstrap so Spring Boot test slices (e.g. {@code @DataJpaTest}) run in this
 * library module without a real application entry point — the catalog module itself never
 * ships a main class, that lives in {@code app}.
 */
@SpringBootApplication
class CatalogTestApplication {
}
