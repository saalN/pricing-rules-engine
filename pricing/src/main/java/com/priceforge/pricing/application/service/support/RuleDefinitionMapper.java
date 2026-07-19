package com.priceforge.pricing.application.service.support;

import com.priceforge.pricing.domain.model.CategoryScope;
import com.priceforge.pricing.domain.model.CustomerSegment;
import com.priceforge.pricing.domain.model.GeoCondition;
import com.priceforge.pricing.domain.model.GlobalScope;
import com.priceforge.pricing.domain.model.PricingRuleType;
import com.priceforge.pricing.domain.model.ProductScope;
import com.priceforge.pricing.domain.model.RuleCondition;
import com.priceforge.pricing.domain.model.RuleScope;
import com.priceforge.pricing.domain.model.SegmentCondition;
import com.priceforge.pricing.domain.model.TimeWindowCondition;
import com.priceforge.pricing.domain.model.VolumeCondition;
import com.priceforge.sharedkernel.ProductId;
import java.time.LocalTime;

/**
 * Shared by {@code CreatePricingRuleService} and {@code SimulatePriceService} — both translate a
 * flat set of "one field per possible rule type/scope" command fields into the same sealed
 * domain types, and duplicating this switch would be pure repetition.
 */
public final class RuleDefinitionMapper {

    private RuleDefinitionMapper() {
    }

    public static RuleCondition toCondition(
            PricingRuleType type,
            LocalTime timeWindowStart,
            LocalTime timeWindowEnd,
            CustomerSegment segment,
            Integer minimumQuantity,
            String zone) {
        return switch (type) {
            case TIME_BASED -> new TimeWindowCondition(timeWindowStart, timeWindowEnd);
            case SEGMENT_BASED -> new SegmentCondition(segment);
            case VOLUME_BASED -> new VolumeCondition(minimumQuantity);
            case GEO_BASED -> new GeoCondition(zone);
        };
    }

    /** A null {@code scopeProductId} and null {@code scopeCategory} means global scope. */
    public static RuleScope toScope(ProductId scopeProductId, String scopeCategory) {
        if (scopeProductId != null) {
            return new ProductScope(scopeProductId);
        }
        if (scopeCategory != null) {
            return new CategoryScope(scopeCategory);
        }
        return new GlobalScope();
    }
}
