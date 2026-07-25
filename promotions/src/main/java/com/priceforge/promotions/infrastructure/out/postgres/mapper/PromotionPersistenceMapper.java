package com.priceforge.promotions.infrastructure.out.postgres.mapper;

import com.priceforge.promotions.domain.model.Discount;
import com.priceforge.promotions.domain.model.EligibilityCriteria;
import com.priceforge.promotions.domain.model.Promotion;
import com.priceforge.promotions.infrastructure.out.postgres.entity.PromotionDocument;
import com.priceforge.sharedkernel.PromotionId;
import org.springframework.stereotype.Component;

@Component
public class PromotionPersistenceMapper {

    /**
     * {@code Promotion} deliberately never carries its own usage count (see the aggregate's own
     * doc) — the caller supplies whatever count is already persisted (0 for a brand-new
     * promotion) so a status-only update (UC-12's activate/expire) never resets it.
     */
    public PromotionDocument toEntity(Promotion promotion, int globalUsageCount) {
        EligibilityCriteria eligibility = promotion.eligibility();
        return new PromotionDocument(
            promotion.id().value(),
            promotion.name(),
            promotion.description(),
            promotion.validFrom(),
            promotion.validUntil(),
            promotion.globalUsageLimit(),
            promotion.perUserUsageLimit(),
            globalUsageCount,
            eligibility.segment(),
            eligibility.category(),
            eligibility.minimumPurchaseAmount(),
            promotion.discount().type(),
            promotion.discount().value(),
            promotion.priority(),
            promotion.combinationMode(),
            promotion.status(),
            promotion.createdAt());
    }

    public Promotion toDomain(PromotionDocument document) {
        EligibilityCriteria eligibility = new EligibilityCriteria(
            document.getSegment(), document.getCategory(), document.getMinimumPurchaseAmount());
        Discount discount = new Discount(document.getDiscountType(), document.getDiscountValue());

        return Promotion.reconstitute(
            new PromotionId(document.getId()),
            document.getName(),
            document.getDescription(),
            document.getValidFrom(),
            document.getValidUntil(),
            document.getGlobalUsageLimit(),
            document.getPerUserUsageLimit(),
            eligibility,
            discount,
            document.getPriority(),
            document.getCombinationMode(),
            document.getStatus(),
            document.getCreatedAt());
    }
}
