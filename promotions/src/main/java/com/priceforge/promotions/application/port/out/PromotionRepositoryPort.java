package com.priceforge.promotions.application.port.out;

import com.priceforge.promotions.domain.model.Promotion;
import com.priceforge.promotions.domain.model.UsageRecordingOutcome;
import com.priceforge.sharedkernel.PromotionId;
import com.priceforge.sharedkernel.UserId;
import java.util.List;
import java.util.Optional;

public interface PromotionRepositoryPort {

    Promotion save(Promotion promotion);

    Optional<Promotion> findById(PromotionId id);

    /** Every ACTIVE promotion — UC-11 applies eligibility, conflict resolution and windowing itself. */
    List<Promotion> findAllActive();

    /** Every SCHEDULED promotion — UC-12's scheduler checks each one's validFrom itself. */
    List<Promotion> findAllScheduled();

    /**
     * UC-11 step 4: checks the global and per-user usage limits and increments both counters in a
     * single atomic operation, so a concurrent request can never bypass a limit reached a moment
     * earlier. Transitions the promotion to EXHAUSTED in that same operation when the increment
     * reaches the global limit.
     */
    UsageRecordingOutcome recordUsageIfWithinLimits(PromotionId promotionId, UserId userId);
}
