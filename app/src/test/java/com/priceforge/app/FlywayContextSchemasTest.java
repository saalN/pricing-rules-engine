package com.priceforge.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class FlywayContextSchemasTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsOneSchemaPerBoundedContext() {
        List<String> schemas = jdbcTemplate.queryForList(
            "SELECT schema_name FROM information_schema.schemata WHERE schema_name IN "
                + "('catalog', 'pricing', 'promotions', 'audit')",
            String.class);

        assertThat(schemas).containsExactlyInAnyOrder("catalog", "pricing", "promotions", "audit");
    }
}
