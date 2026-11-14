package com.priceforge.audit;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test-only bootstrap so Spring Boot test slices (e.g. {@code @DataJpaTest}, {@code @WebMvcTest})
 * run in this library module without a real application entry point — the audit module itself
 * never ships a main class, that lives in {@code app}.
 */
@SpringBootApplication
class AuditTestApplication {
}
