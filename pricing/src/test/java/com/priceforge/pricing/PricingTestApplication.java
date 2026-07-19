package com.priceforge.pricing;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test-only bootstrap so Spring Boot test slices (e.g. {@code @DataJpaTest}, {@code @WebMvcTest})
 * run in this library module without a real application entry point — that lives in {@code app}.
 */
@SpringBootApplication
class PricingTestApplication {
}
