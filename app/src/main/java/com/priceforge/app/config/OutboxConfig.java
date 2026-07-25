package com.priceforge.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceforge.app.outbox.JdbcOutboxEventPublisher;
import com.priceforge.sharedkernel.EventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * One {@link EventPublisher} bean per bounded context, each writing to that context's own
 * outbox_event schema — never a single shared instance, since services must be wired to their
 * own context's outbox via {@code @Qualifier}.
 */
@Configuration
public class OutboxConfig {

    @Bean("catalogEventPublisher")
    public EventPublisher catalogEventPublisher(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        return new JdbcOutboxEventPublisher(jdbcTemplate, objectMapper, "catalog");
    }

    @Bean("pricingEventPublisher")
    public EventPublisher pricingEventPublisher(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        return new JdbcOutboxEventPublisher(jdbcTemplate, objectMapper, "pricing");
    }

    @Bean("promotionsEventPublisher")
    public EventPublisher promotionsEventPublisher(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        return new JdbcOutboxEventPublisher(jdbcTemplate, objectMapper, "promotions");
    }
}
