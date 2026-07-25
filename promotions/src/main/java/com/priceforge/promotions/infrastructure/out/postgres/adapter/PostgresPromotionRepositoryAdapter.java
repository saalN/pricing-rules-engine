package com.priceforge.promotions.infrastructure.out.postgres.adapter;

import com.priceforge.promotions.application.port.out.PromotionRepositoryPort;
import com.priceforge.promotions.domain.model.Promotion;
import com.priceforge.promotions.domain.model.PromotionStatus;
import com.priceforge.promotions.domain.model.UsageRecordingOutcome;
import com.priceforge.promotions.infrastructure.out.postgres.mapper.PromotionPersistenceMapper;
import com.priceforge.promotions.infrastructure.out.postgres.repository.SpringDataPromotionPostgresRepository;
import com.priceforge.sharedkernel.PromotionId;
import com.priceforge.sharedkernel.UserId;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class PostgresPromotionRepositoryAdapter implements PromotionRepositoryPort {

    private final SpringDataPromotionPostgresRepository springDataRepository;
    private final PromotionPersistenceMapper mapper;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;

    /**
     * {@code recordUsageIfWithinLimits} manages its own transaction programmatically via
     * {@link TransactionTemplate} rather than {@code @Transactional} — the latter only takes
     * effect through a Spring AOP proxy, but this adapter (like every persistence adapter in this
     * repo) is instantiated directly in tests to sidestep {@code @DataJpaTest}'s component scan,
     * so an annotation alone would silently do nothing there and leave the row locks below
     * protecting nothing. {@link TransactionTemplate} establishes a real transaction boundary
     * (joining an ambient one if the caller already started one, e.g. {@code PromotionApplierService}) regardless of how this class was instantiated.
     */
    public PostgresPromotionRepositoryAdapter(
            SpringDataPromotionPostgresRepository springDataRepository,
            PromotionPersistenceMapper mapper,
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            EntityManager entityManager) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.entityManager = entityManager;
    }

    @Override
    public Promotion save(Promotion promotion) {
        int existingUsageCount = springDataRepository.findById(promotion.id().value())
            .map(document -> document.getGlobalUsageCount())
            .orElse(0);
        // Flushed immediately: recordUsageIfWithinLimits reads this same row back via raw JDBC in
        // the same transaction, which would not see a write Hibernate is still buffering.
        return mapper.toDomain(springDataRepository.saveAndFlush(mapper.toEntity(promotion, existingUsageCount)));
    }

    @Override
    public Optional<Promotion> findById(PromotionId id) {
        return springDataRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Promotion> findAllActive() {
        return springDataRepository.findAllByStatus(PromotionStatus.ACTIVE).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Promotion> findAllScheduled() {
        return springDataRepository.findAllByStatus(PromotionStatus.SCHEDULED).stream()
            .map(mapper::toDomain)
            .toList();
    }

    /**
     * UC-11: {@code SELECT ... FOR UPDATE} locks the promotion row (and the user's usage row, if
     * it already exists) for the rest of this transaction, so a concurrent transaction checking
     * the same promotion/user pair blocks until this one commits its increment — the atomicity
     * the functional spec requires, achieved with row-level locking rather than a single UPDATE
     * statement, since the global and per-user limits live in two different tables.
     */
    @Override
    public UsageRecordingOutcome recordUsageIfWithinLimits(PromotionId promotionId, UserId userId) {
        return transactionTemplate.execute(status -> doRecordUsageIfWithinLimits(promotionId, userId));
    }

    private UsageRecordingOutcome doRecordUsageIfWithinLimits(PromotionId promotionId, UserId userId) {
        Map<String, Object> promotionRow = jdbcTemplate.queryForMap(
            "SELECT global_usage_count, global_usage_limit, per_user_usage_limit, status "
                + "FROM promotions.promotion WHERE id = ? FOR UPDATE",
            promotionId.value());
        int globalCount = ((Number) promotionRow.get("global_usage_count")).intValue();
        int globalLimit = ((Number) promotionRow.get("global_usage_limit")).intValue();
        int perUserLimit = ((Number) promotionRow.get("per_user_usage_limit")).intValue();

        if (globalCount >= globalLimit) {
            return UsageRecordingOutcome.GLOBAL_LIMIT_REACHED;
        }

        List<Map<String, Object>> usageRows = jdbcTemplate.queryForList(
            "SELECT usage_count FROM promotions.promotion_usage WHERE promotion_id = ? AND user_id = ? FOR UPDATE",
            promotionId.value(), userId.value());
        int userCount = usageRows.isEmpty() ? 0 : ((Number) usageRows.get(0).get("usage_count")).intValue();

        if (userCount >= perUserLimit) {
            return UsageRecordingOutcome.PER_USER_LIMIT_REACHED;
        }

        int newGlobalCount = globalCount + 1;
        boolean nowExhausted = newGlobalCount >= globalLimit;

        jdbcTemplate.update(
            "UPDATE promotions.promotion SET global_usage_count = ?, status = ? WHERE id = ?",
            newGlobalCount, nowExhausted ? PromotionStatus.EXHAUSTED.name() : promotionRow.get("status"),
            promotionId.value());

        jdbcTemplate.update(
            "INSERT INTO promotions.promotion_usage (promotion_id, user_id, usage_count) VALUES (?, ?, 1) "
                + "ON CONFLICT (promotion_id, user_id) "
                + "DO UPDATE SET usage_count = promotions.promotion_usage.usage_count + 1",
            promotionId.value(), userId.value());

        // This raw JDBC write bypasses Hibernate's persistence context entirely — without
        // clearing it, a later JPA read of this same row in this transaction (e.g. save()'s own
        // findById lookup) would return the stale pre-update entity from the first-level cache.
        entityManager.clear();

        return nowExhausted ? UsageRecordingOutcome.APPLIED_AND_NOW_EXHAUSTED : UsageRecordingOutcome.APPLIED;
    }
}
