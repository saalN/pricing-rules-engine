package com.priceforge.pricing.infrastructure.out.postgres.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.sharedkernel.CustomerSegment;
import com.priceforge.pricing.domain.model.Discount;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.GlobalScope;
import com.priceforge.pricing.domain.model.PricingRule;
import com.priceforge.pricing.domain.model.PricingRuleStatus;
import com.priceforge.pricing.domain.model.PricingRuleType;
import com.priceforge.pricing.domain.model.SegmentCondition;
import com.priceforge.pricing.infrastructure.out.postgres.mapper.PricingRulePersistenceMapper;
import com.priceforge.pricing.infrastructure.out.postgres.repository.SpringDataPricingRulePostgresRepository;
import com.priceforge.sharedkernel.PricingRuleId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
class PostgresPricingRuleRepositoryAdapterTest {

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
    static void migratePricingSchema() {
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .schemas("pricing")
            .locations("classpath:db/migration/pricing")
            .load()
            .migrate();
    }

    @Autowired
    private SpringDataPricingRulePostgresRepository springDataRepository;

    private PostgresPricingRuleRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PostgresPricingRuleRepositoryAdapter(springDataRepository, new PricingRulePersistenceMapper());
    }

    private PricingRule sampleRule() {
        return PricingRule.create(
            PricingRuleId.newId(), PricingRuleType.SEGMENT_BASED, new SegmentCondition(CustomerSegment.VIP),
            new GlobalScope(), new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(20)),
            10, CombinationMode.STACKABLE, Instant.now());
    }

    @Test
    void savesAndReloadsARulePreservingAllFields() {
        PricingRule rule = sampleRule();

        adapter.save(rule);
        Optional<PricingRule> reloaded = adapter.findById(rule.id());

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().type()).isEqualTo(PricingRuleType.SEGMENT_BASED);
        assertThat(reloaded.get().priority()).isEqualTo(10);
        assertThat(reloaded.get().combinationMode()).isEqualTo(CombinationMode.STACKABLE);
        assertThat(reloaded.get().discount().value()).isEqualByComparingTo(BigDecimal.valueOf(20));
        assertThat(reloaded.get().status()).isEqualTo(PricingRuleStatus.ACTIVE);
    }

    @Test
    void findByIdReturnsEmptyWhenTheRuleDoesNotExist() {
        assertThat(adapter.findById(PricingRuleId.newId())).isEmpty();
    }

    @Test
    void findAllActiveExcludesDeactivatedRules() {
        PricingRule active = sampleRule();
        PricingRule inactive = sampleRule();
        inactive.deactivate();

        adapter.save(active);
        adapter.save(inactive);

        List<PricingRule> activeRules = adapter.findAllActive();

        assertThat(activeRules).extracting(PricingRule::id).contains(active.id()).doesNotContain(inactive.id());
    }
}
