package com.priceforge.promotions.application.service.support;

import com.priceforge.promotions.application.port.out.PromotionRepositoryPort;
import com.priceforge.promotions.domain.model.Promotion;
import com.priceforge.promotions.domain.model.PromotionStatus;
import com.priceforge.promotions.domain.model.UsageRecordingOutcome;
import com.priceforge.sharedkernel.PromotionId;
import com.priceforge.sharedkernel.UserId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory stand-in for {@link PromotionRepositoryPort} used by application-layer tests. Usage
 * counters are plain fields, not thread-safe — fine for single-threaded tests, unlike the real
 * Postgres adapter which must guarantee atomicity under concurrency.
 */
public final class InMemoryPromotionRepositoryPort implements PromotionRepositoryPort {

    private final Map<PromotionId, Promotion> promotionsById = new HashMap<>();
    private final Map<PromotionId, Integer> globalUsageCounts = new HashMap<>();
    private final Map<PromotionId, Map<UserId, Integer>> perUserUsageCounts = new HashMap<>();

    @Override
    public Promotion save(Promotion promotion) {
        promotionsById.put(promotion.id(), promotion);
        return promotion;
    }

    @Override
    public Optional<Promotion> findById(PromotionId id) {
        return Optional.ofNullable(promotionsById.get(id));
    }

    @Override
    public List<Promotion> findAllActive() {
        return promotionsById.values().stream().filter(p -> p.status() == PromotionStatus.ACTIVE).toList();
    }

    @Override
    public List<Promotion> findAllScheduled() {
        return promotionsById.values().stream().filter(p -> p.status() == PromotionStatus.SCHEDULED).toList();
    }

    @Override
    public UsageRecordingOutcome recordUsageIfWithinLimits(PromotionId promotionId, UserId userId) {
        Promotion promotion = promotionsById.get(promotionId);
        int globalCount = globalUsageCounts.getOrDefault(promotionId, 0);
        Map<UserId, Integer> perUser = perUserUsageCounts.computeIfAbsent(promotionId, id -> new HashMap<>());
        int userCount = perUser.getOrDefault(userId, 0);

        if (globalCount >= promotion.globalUsageLimit()) {
            return UsageRecordingOutcome.GLOBAL_LIMIT_REACHED;
        }
        if (userCount >= promotion.perUserUsageLimit()) {
            return UsageRecordingOutcome.PER_USER_LIMIT_REACHED;
        }

        int newGlobalCount = globalCount + 1;
        globalUsageCounts.put(promotionId, newGlobalCount);
        perUser.put(userId, userCount + 1);

        if (newGlobalCount >= promotion.globalUsageLimit()) {
            promotion.exhaust();
            return UsageRecordingOutcome.APPLIED_AND_NOW_EXHAUSTED;
        }
        return UsageRecordingOutcome.APPLIED;
    }
}
