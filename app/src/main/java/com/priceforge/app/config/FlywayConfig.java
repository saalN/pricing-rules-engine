package com.priceforge.app.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Runs one Flyway instance per bounded-context schema — its own migration location and its own
 * {@code flyway_schema_history} table inside that schema — instead of Spring Boot's single
 * auto-configured instance. This is what the "one Postgres schema per bounded context" decision
 * requires; {@code spring.flyway.enabled=false} turns off the default.
 */
@Configuration
public class FlywayConfig {

    private static final String[] CONTEXT_SCHEMAS = {"catalog", "pricing", "promotions", "audit"};

    @Bean
    @Order(0)
    public CommandLineRunner migrateContextSchemas(DataSource dataSource) {
        return args -> {
            for (String schema : CONTEXT_SCHEMAS) {
                Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schema)
                    .locations("classpath:db/migration/" + schema)
                    .table("flyway_schema_history")
                    .load()
                    .migrate();
            }
        };
    }
}
