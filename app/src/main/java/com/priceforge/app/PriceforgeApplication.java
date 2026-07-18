package com.priceforge.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Data JPA's repository and entity scanning don't follow {@code scanBasePackages} — they
 * default to this class's own package — so both must be pointed at {@code com.priceforge}
 * explicitly to pick up each bounded context's persistence adapter.
 */
@SpringBootApplication(scanBasePackages = "com.priceforge")
@EnableJpaRepositories(basePackages = "com.priceforge")
@EntityScan(basePackages = "com.priceforge")
@EnableScheduling
public class PriceforgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(PriceforgeApplication.class, args);
    }
}
