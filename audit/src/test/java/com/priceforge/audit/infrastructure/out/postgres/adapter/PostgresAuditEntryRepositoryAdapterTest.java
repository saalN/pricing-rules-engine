package com.priceforge.audit.infrastructure.out.postgres.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.audit.application.dto.AuditEntryFilter;
import com.priceforge.audit.application.dto.AuditEntrySearchResult;
import com.priceforge.audit.application.service.support.AuditEntries;
import com.priceforge.audit.domain.model.AuditEntityType;
import com.priceforge.audit.domain.model.AuditEntry;
import com.priceforge.audit.infrastructure.out.postgres.mapper.AuditEntryPersistenceMapper;
import com.priceforge.audit.infrastructure.out.postgres.repository.SpringDataAuditEntryPostgresRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostgresAuditEntryRepositoryAdapterTest {

    private static final Instant T1 = Instant.parse("2026-07-20T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-07-21T10:00:00Z");
    private static final Instant T3 = Instant.parse("2026-07-22T10:00:00Z");

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
    static void migrateAuditSchema() {
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .schemas("audit")
            .locations("classpath:db/migration/audit")
            .load()
            .migrate();
    }

    @Autowired
    private SpringDataAuditEntryPostgresRepository springDataRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PostgresAuditEntryRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PostgresAuditEntryRepositoryAdapter(springDataRepository, new AuditEntryPersistenceMapper());
    }

    @Test
    void savesAndReloadsAPriceCalculationWithItsSnapshotAndExactPayload() {
        String productId = UUID.randomUUID().toString();
        AuditEntry entry = AuditEntries.priceCalculation(
            productId, UUID.randomUUID().toString(), T1, new BigDecimal("100.0000"), new BigDecimal("85.5000"));

        adapter.saveIfAbsent(entry);
        flush();

        AuditEntry reloaded = adapter.findById(entry.eventId()).orElseThrow();
        assertThat(reloaded.entityType()).isEqualTo(AuditEntityType.PRICE_CALCULATION);
        assertThat(reloaded.productId()).isEqualTo(productId);
        assertThat(reloaded.occurredAt()).isEqualTo(T1);
        assertThat(reloaded.priceSnapshot().finalPriceAmount()).isEqualByComparingTo("85.5");
        assertThat(reloaded.priceSnapshot().userSegment()).isEqualTo("VIP");
        assertThat(reloaded.priceSnapshot().zoneId()).isEqualTo("UTC");
        assertThat(reloaded.priceSnapshot().requestedAt()).isEqualTo(T1);
    }

    @Test
    void storesThePayloadAsAJsonbObjectNotAnEscapedString() {
        AuditEntry entry = AuditEntries.change(
            AuditEntityType.PRICING_RULE, "rule-json", "PricingRuleUpdated", T1, "10%", "15%", "admin");

        adapter.saveIfAbsent(entry);
        flush();

        String type = jdbcTemplate.queryForObject(
            "SELECT jsonb_typeof(payload) FROM audit.audit_entry WHERE event_id = ?", String.class, entry.eventId());
        String aggregateId = jdbcTemplate.queryForObject(
            "SELECT payload->>'aggregateId' FROM audit.audit_entry WHERE event_id = ?", String.class, entry.eventId());
        assertThat(type).isEqualTo("object");
        assertThat(aggregateId).isEqualTo("rule-json");
    }

    @Test
    void recordingTheSameEventTwiceKeepsASingleRow() {
        AuditEntry entry = AuditEntries.change(
            AuditEntityType.PROMOTION, "promo-dup", "PromotionActivated", T1, null, "ACTIVE", null);

        adapter.saveIfAbsent(entry);
        flush();
        adapter.saveIfAbsent(entry);
        flush();

        assertThat(adapter.search(
                new AuditEntryFilter(AuditEntityType.PROMOTION, "promo-dup", null, null, null, null), 0, 10, false)
            .totalElements()).isEqualTo(1);
    }

    @Test
    void findByIdReturnsEmptyForAnUnknownEvent() {
        assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void searchesByProductUserAndDateRangeNewestFirstWithPaging() {
        String product = "product-search";
        adapter.saveIfAbsent(AuditEntries.priceCalculation(product, "user-a", T1, BigDecimal.TEN, BigDecimal.ONE));
        adapter.saveIfAbsent(AuditEntries.priceCalculation(product, "user-b", T2, BigDecimal.TEN, BigDecimal.ONE));
        adapter.saveIfAbsent(AuditEntries.priceCalculation(product, "user-a", T3, BigDecimal.TEN, BigDecimal.ONE));
        adapter.saveIfAbsent(AuditEntries.priceCalculation("other-product", "user-a", T2, BigDecimal.TEN, BigDecimal.ONE));
        flush();

        AuditEntryFilter byProduct = filter(product, null, null, null);
        AuditEntrySearchResult all = adapter.search(byProduct, 0, 10, true);
        AuditEntrySearchResult secondPage = adapter.search(byProduct, 1, 2, true);
        AuditEntrySearchResult byUser = adapter.search(filter(product, "user-a", null, null), 0, 10, true);
        AuditEntrySearchResult byRange = adapter.search(filter(product, null, T2, T3), 0, 10, false);

        assertThat(all.totalElements()).isEqualTo(3);
        assertThat(all.entries()).extracting(AuditEntry::occurredAt).containsExactly(T3, T2, T1);
        assertThat(secondPage.totalElements()).isEqualTo(3);
        assertThat(secondPage.entries()).extracting(AuditEntry::occurredAt).containsExactly(T1);
        assertThat(byUser.totalElements()).isEqualTo(2);
        assertThat(byRange.entries()).extracting(AuditEntry::occurredAt).containsExactly(T2, T3);
    }

    @Test
    void searchWithNoFilterReturnsEveryEntryOfAnyType() {
        adapter.saveIfAbsent(AuditEntries.change(
            AuditEntityType.PRODUCT, "p-any", "ProductDeactivated", T1, "ACTIVE", "INACTIVE", null));
        adapter.saveIfAbsent(AuditEntries.change(
            AuditEntityType.PROMOTION, "promo-any", "PromotionExpired", T1, null, "EXPIRED", null));
        flush();

        List<AuditEntry> entries = adapter.search(
            new AuditEntryFilter(null, null, null, null, null, null), 0, 1000, false).entries();

        assertThat(entries).extracting(AuditEntry::entityId).contains("p-any", "promo-any");
    }

    // Two tests, not one: a rejected statement aborts the surrounding test transaction in Postgres.
    @Test
    void theDatabaseRejectsUpdatesOfStoredEntries() {
        AuditEntry entry = storedEntry("p-immutable-update");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE audit.audit_entry SET new_state = 'tampered' WHERE event_id = ?", entry.eventId()))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("append-only");
    }

    @Test
    void theDatabaseRejectsDeletesOfStoredEntries() {
        AuditEntry entry = storedEntry("p-immutable-delete");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM audit.audit_entry WHERE event_id = ?", entry.eventId()))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("append-only");
    }

    private AuditEntry storedEntry(String entityId) {
        AuditEntry entry = AuditEntries.change(
            AuditEntityType.PRODUCT, entityId, "ProductDeactivated", T1, "ACTIVE", "INACTIVE", null);
        adapter.saveIfAbsent(entry);
        flush();
        return entry;
    }

    private AuditEntryFilter filter(String productId, String userId, Instant from, Instant to) {
        return new AuditEntryFilter(AuditEntityType.PRICE_CALCULATION, null, productId, userId, from, to);
    }

    private void flush() {
        springDataRepository.flush();
    }
}
