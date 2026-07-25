package com.priceforge.promotions.infrastructure.out.postgres.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.priceforge.pricing.domain.model.CustomerSegment;
import com.priceforge.promotions.domain.model.Discount;
import com.priceforge.promotions.domain.model.DiscountType;
import com.priceforge.promotions.domain.model.EligibilityCriteria;
import com.priceforge.promotions.domain.model.Promotion;
import com.priceforge.promotions.domain.model.PromotionStatus;
import com.priceforge.promotions.domain.model.UsageRecordingOutcome;
import com.priceforge.promotions.infrastructure.out.postgres.mapper.PromotionPersistenceMapper;
import com.priceforge.promotions.infrastructure.out.postgres.repository.SpringDataPromotionPostgresRepository;
import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.sharedkernel.PromotionId;
import com.priceforge.sharedkernel.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostgresPromotionRepositoryAdapterTest {

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
    static void migratePromotionsSchema() {
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .schemas("promotions")
            .locations("classpath:db/migration/promotions")
            .load()
            .migrate();
    }

    @Autowired
    private SpringDataPromotionPostgresRepository springDataRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private PostgresPromotionRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PostgresPromotionRepositoryAdapter(
            springDataRepository, new PromotionPersistenceMapper(), jdbcTemplate, transactionManager, entityManager);
        jdbcTemplate.update("DELETE FROM promotions.promotion_usage");
        jdbcTemplate.update("DELETE FROM promotions.promotion");
    }

    private static final EligibilityCriteria NO_CONSTRAINTS = new EligibilityCriteria(null, null, null);

    private Promotion samplePromotion(int globalLimit, int perUserLimit) {
        Instant now = Instant.now();
        return Promotion.create(
            PromotionId.newId(), "Summer sale", "10% off", now.minus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS),
            globalLimit, perUserLimit, NO_CONSTRAINTS, new Discount(DiscountType.PERCENTAGE, BigDecimal.TEN),
            10, CombinationMode.STACKABLE, now);
    }

    @Test
    void savesAndReloadsAPromotionPreservingAllFields() {
        Promotion promotion = samplePromotion(1000, 1);

        adapter.save(promotion);
        Optional<Promotion> reloaded = adapter.findById(promotion.id());

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().name()).isEqualTo("Summer sale");
        assertThat(reloaded.get().globalUsageLimit()).isEqualTo(1000);
        assertThat(reloaded.get().perUserUsageLimit()).isEqualTo(1);
        assertThat(reloaded.get().status()).isEqualTo(PromotionStatus.ACTIVE);
        assertThat(reloaded.get().combinationMode()).isEqualTo(CombinationMode.STACKABLE);
    }

    @Test
    void savingAStatusTransitionNeverResetsTheGlobalUsageCount() {
        Promotion promotion = samplePromotion(5, 5);
        adapter.save(promotion);
        adapter.recordUsageIfWithinLimits(promotion.id(), UserId.newId());
        adapter.recordUsageIfWithinLimits(promotion.id(), UserId.newId());

        // A later save() (e.g. UC-12's scheduler expiring the promotion) must not reset the
        // counter incremented by the atomic usage operation above.
        Promotion reloaded = adapter.findById(promotion.id()).orElseThrow();
        reloaded.expire();
        adapter.save(reloaded);

        int usageCount = jdbcTemplate.queryForObject(
            "SELECT global_usage_count FROM promotions.promotion WHERE id = ?", Integer.class, promotion.id().value());
        assertThat(usageCount).isEqualTo(2);
        assertThat(adapter.findById(promotion.id()).orElseThrow().status()).isEqualTo(PromotionStatus.EXPIRED);
    }

    @Test
    void findsOnlyPromotionsMatchingTheRequestedStatus() {
        Instant now = Instant.now();
        Promotion active = samplePromotion(1000, 1);
        Promotion scheduled = Promotion.create(
            PromotionId.newId(), "Future sale", "d", now.plus(1, ChronoUnit.DAYS), now.plus(2, ChronoUnit.DAYS),
            1000, 1, NO_CONSTRAINTS, new Discount(DiscountType.PERCENTAGE, BigDecimal.TEN), 10,
            CombinationMode.STACKABLE, now);
        adapter.save(active);
        adapter.save(scheduled);

        List<Promotion> activeOnes = adapter.findAllActive();
        List<Promotion> scheduledOnes = adapter.findAllScheduled();

        assertThat(activeOnes).extracting(Promotion::id).containsExactly(active.id());
        assertThat(scheduledOnes).extracting(Promotion::id).containsExactly(scheduled.id());
    }

    @Test
    void appliesAndIncrementsUsageWithinLimits() {
        Promotion promotion = samplePromotion(1000, 2);
        adapter.save(promotion);
        UserId userId = UserId.newId();

        UsageRecordingOutcome first = adapter.recordUsageIfWithinLimits(promotion.id(), userId);
        UsageRecordingOutcome second = adapter.recordUsageIfWithinLimits(promotion.id(), userId);

        assertThat(first).isEqualTo(UsageRecordingOutcome.APPLIED);
        assertThat(second).isEqualTo(UsageRecordingOutcome.APPLIED);
    }

    @Test
    void reachingThePerUserLimitStopsThatUserButNotOthers() {
        Promotion promotion = samplePromotion(1000, 1);
        adapter.save(promotion);
        UserId userA = UserId.newId();
        UserId userB = UserId.newId();

        assertThat(adapter.recordUsageIfWithinLimits(promotion.id(), userA)).isEqualTo(UsageRecordingOutcome.APPLIED);
        assertThat(adapter.recordUsageIfWithinLimits(promotion.id(), userA))
            .isEqualTo(UsageRecordingOutcome.PER_USER_LIMIT_REACHED);
        assertThat(adapter.recordUsageIfWithinLimits(promotion.id(), userB)).isEqualTo(UsageRecordingOutcome.APPLIED);
    }

    @Test
    void reachingTheGlobalLimitExhaustsThePromotionAndBlocksFurtherUsage() {
        Promotion promotion = samplePromotion(1, 5);
        adapter.save(promotion);

        UsageRecordingOutcome outcome = adapter.recordUsageIfWithinLimits(promotion.id(), UserId.newId());
        UsageRecordingOutcome next = adapter.recordUsageIfWithinLimits(promotion.id(), UserId.newId());

        assertThat(outcome).isEqualTo(UsageRecordingOutcome.APPLIED_AND_NOW_EXHAUSTED);
        assertThat(next).isEqualTo(UsageRecordingOutcome.GLOBAL_LIMIT_REACHED);
        assertThat(adapter.findById(promotion.id()).orElseThrow().status()).isEqualTo(PromotionStatus.EXHAUSTED);
    }

    /**
     * The surrounding {@code @DataJpaTest} normally wraps each test method in one rollback-only
     * transaction on the test's own thread — {@code NOT_SUPPORTED} suspends that here, since the
     * setup write below must actually commit for the two worker threads (each on their own
     * connection/transaction) to see it at all.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentRequestsForTheSameGlobalLimitOfOneNeverBothSucceed() throws InterruptedException {
        Promotion promotion = samplePromotion(1, 100);
        adapter.save(promotion);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        UsageRecordingOutcome[] outcomes = new UsageRecordingOutcome[2];

        for (int i = 0; i < 2; i++) {
            int index = i;
            executor.submit(() -> {
                PostgresPromotionRepositoryAdapter threadAdapter = new PostgresPromotionRepositoryAdapter(
                    springDataRepository, new PromotionPersistenceMapper(), jdbcTemplate, transactionManager,
                    entityManager);
                ready.countDown();
                try {
                    go.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                outcomes[index] = threadAdapter.recordUsageIfWithinLimits(promotion.id(), UserId.newId());
            });
        }

        ready.await();
        go.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long succeeded = List.of(outcomes).stream()
            .filter(outcome -> outcome == UsageRecordingOutcome.APPLIED_AND_NOW_EXHAUSTED)
            .count();
        long limitReached = List.of(outcomes).stream()
            .filter(outcome -> outcome == UsageRecordingOutcome.GLOBAL_LIMIT_REACHED)
            .count();

        assertThat(succeeded).isEqualTo(1);
        assertThat(limitReached).isEqualTo(1);
    }
}
