package com.priceforge.pricing.infrastructure.out.postgres.mapper;

import com.priceforge.pricing.application.service.support.RuleDefinitionMapper;
import com.priceforge.pricing.domain.model.CategoryScope;
import com.priceforge.pricing.domain.model.Discount;
import com.priceforge.pricing.domain.model.GeoCondition;
import com.priceforge.pricing.domain.model.PricingRule;
import com.priceforge.pricing.domain.model.ProductScope;
import com.priceforge.pricing.domain.model.RuleCondition;
import com.priceforge.pricing.domain.model.RuleScope;
import com.priceforge.pricing.domain.model.SegmentCondition;
import com.priceforge.pricing.domain.model.TimeWindowCondition;
import com.priceforge.pricing.domain.model.VolumeCondition;
import com.priceforge.pricing.infrastructure.out.postgres.entity.PricingRuleDocument;
import com.priceforge.sharedkernel.PricingRuleId;
import com.priceforge.sharedkernel.ProductId;
import org.springframework.stereotype.Component;

@Component
public class PricingRulePersistenceMapper {

    public PricingRuleDocument toEntity(PricingRule rule) {
        RuleCondition condition = rule.condition();
        TimeWindowCondition timeWindow = condition instanceof TimeWindowCondition c ? c : null;

        return new PricingRuleDocument(
            rule.id().value(),
            rule.type(),
            timeWindow != null ? timeWindow.startTime() : null,
            timeWindow != null ? timeWindow.endTime() : null,
            condition instanceof SegmentCondition c ? c.segment() : null,
            condition instanceof VolumeCondition c ? c.minimumQuantity() : null,
            condition instanceof GeoCondition c ? c.zone() : null,
            rule.discount().type(),
            rule.discount().value(),
            rule.priority(),
            rule.combinationMode(),
            rule.scope() instanceof ProductScope s ? s.productId().value() : null,
            rule.scope() instanceof CategoryScope s ? s.category() : null,
            rule.status(),
            rule.createdAt());
    }

    public PricingRule toDomain(PricingRuleDocument document) {
        RuleCondition condition = RuleDefinitionMapper.toCondition(
            document.getType(), document.getTimeWindowStart(), document.getTimeWindowEnd(),
            document.getSegment(), document.getMinimumQuantity(), document.getZone());
        RuleScope scope = RuleDefinitionMapper.toScope(
            document.getScopeProductId() != null ? new ProductId(document.getScopeProductId()) : null,
            document.getScopeCategory());
        Discount discount = new Discount(document.getDiscountType(), document.getDiscountValue());

        return PricingRule.reconstitute(
            new PricingRuleId(document.getId()),
            document.getType(),
            condition,
            scope,
            discount,
            document.getPriority(),
            document.getCombinationMode(),
            document.getStatus(),
            document.getCreatedAt());
    }
}
