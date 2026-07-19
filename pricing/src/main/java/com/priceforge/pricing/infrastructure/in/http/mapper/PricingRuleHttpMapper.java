package com.priceforge.pricing.infrastructure.in.http.mapper;

import com.priceforge.pricing.application.dto.CreatePricingRuleCommand;
import com.priceforge.pricing.application.dto.CreatePricingRuleResult;
import com.priceforge.pricing.application.dto.DeactivatePricingRuleResult;
import com.priceforge.pricing.application.dto.UpdatePricingRuleCommand;
import com.priceforge.pricing.application.dto.UpdatePricingRuleResult;
import com.priceforge.pricing.infrastructure.in.http.dto.CreatePricingRuleResponse;
import com.priceforge.pricing.infrastructure.in.http.dto.DeactivatePricingRuleResponse;
import com.priceforge.pricing.infrastructure.in.http.dto.PricingRuleDefinitionRequest;
import com.priceforge.pricing.infrastructure.in.http.dto.UpdatePricingRuleRequest;
import com.priceforge.pricing.infrastructure.in.http.dto.UpdatePricingRuleResponse;
import com.priceforge.sharedkernel.PricingRuleId;
import com.priceforge.sharedkernel.ProductId;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PricingRuleHttpMapper {

    public CreatePricingRuleCommand toCommand(PricingRuleDefinitionRequest request) {
        return new CreatePricingRuleCommand(
            request.type(),
            request.timeWindowStart(),
            request.timeWindowEnd(),
            request.segment(),
            request.minimumQuantity(),
            request.zone(),
            request.discountType(),
            request.discountValue(),
            request.priority(),
            request.combinationMode(),
            request.scopeProductId() != null ? new ProductId(request.scopeProductId()) : null,
            request.scopeCategory());
    }

    public CreatePricingRuleResponse toResponse(CreatePricingRuleResult result) {
        return new CreatePricingRuleResponse(result.ruleId().value());
    }

    public UpdatePricingRuleCommand toCommand(UUID ruleId, UpdatePricingRuleRequest request, String changedBy) {
        return new UpdatePricingRuleCommand(
            new PricingRuleId(ruleId),
            request.timeWindowStart(),
            request.timeWindowEnd(),
            request.segment(),
            request.minimumQuantity(),
            request.zone(),
            request.discountType(),
            request.discountValue(),
            request.priority(),
            request.combinationMode(),
            changedBy);
    }

    public UpdatePricingRuleResponse toResponse(UpdatePricingRuleResult result) {
        return new UpdatePricingRuleResponse(result.ruleId().value(), result.previousState(), result.newState());
    }

    public DeactivatePricingRuleResponse toResponse(DeactivatePricingRuleResult result) {
        return new DeactivatePricingRuleResponse(result.ruleId().value(), result.status().name());
    }
}
