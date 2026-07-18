package com.priceforge.catalog.infrastructure.out.postgres.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.priceforge.catalog.domain.model.Product;
import com.priceforge.catalog.domain.model.ProductStatus;
import com.priceforge.catalog.infrastructure.out.postgres.mapper.ProductPersistenceMapper;
import com.priceforge.catalog.infrastructure.out.postgres.repository.SpringDataProductPostgresRepository;
import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.ProductId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostgresProductRepositoryAdapterTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeAll
    static void migrateCatalogSchema() {
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .schemas("catalog")
            .locations("classpath:db/migration/catalog")
            .load()
            .migrate();
    }

    @Autowired
    private SpringDataProductPostgresRepository springDataRepository;

    private PostgresProductRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PostgresProductRepositoryAdapter(springDataRepository, new ProductPersistenceMapper());
    }

    @Test
    void savesAndReloadsAProductPreservingAllFields() {
        Product product = Product.create(
            ProductId.newId(),
            "SKU-1",
            "Widget",
            "A widget",
            Money.of(BigDecimal.TEN, "EUR"),
            "URBAN",
            Map.of("weight_kg", 1.5, "zone", "URBAN"),
            Instant.now());

        adapter.save(product);

        Optional<Product> reloaded = adapter.findById(product.id());

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().sku()).isEqualTo("SKU-1");
        assertThat(reloaded.get().basePrice()).isEqualTo(Money.of(BigDecimal.TEN, "EUR"));
        assertThat(reloaded.get().category()).isEqualTo("URBAN");
        assertThat(reloaded.get().customAttributes())
            .containsEntry("weight_kg", 1.5)
            .containsEntry("zone", "URBAN");
        assertThat(reloaded.get().status()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void findByIdReturnsEmptyWhenTheProductDoesNotExist() {
        assertThat(adapter.findById(ProductId.newId())).isEmpty();
    }

    @Test
    void existsBySkuReflectsWhatHasBeenSaved() {
        assertThat(adapter.existsBySku("SKU-2")).isFalse();

        adapter.save(Product.create(
            ProductId.newId(), "SKU-2", "Gadget", "A gadget",
            Money.of(BigDecimal.ONE, "EUR"), "RURAL", Map.of(), Instant.now()));

        assertThat(adapter.existsBySku("SKU-2")).isTrue();
    }
}
