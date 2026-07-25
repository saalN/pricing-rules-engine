package com.priceforge.pricing.infrastructure.in.http.mapper;

import com.priceforge.pricing.application.dto.CalculatePriceCommand;
import com.priceforge.pricing.application.dto.CalculatePriceResult;
import com.priceforge.pricing.application.dto.HypotheticalPricingRule;
import com.priceforge.pricing.application.dto.SimulatePriceCommand;
import com.priceforge.pricing.infrastructure.in.http.dto.AppliedPromotionResponse;
import com.priceforge.pricing.infrastructure.in.http.dto.AppliedRuleResponse;
import com.priceforge.pricing.infrastructure.in.http.dto.CalculatePriceRequest;
import com.priceforge.pricing.infrastructure.in.http.dto.ExcludedPromotionResponse;
import com.priceforge.pricing.infrastructure.in.http.dto.ExcludedRuleResponse;
import com.priceforge.pricing.infrastructure.in.http.dto.PriceQuoteResponse;
import com.priceforge.pricing.infrastructure.in.http.dto.PricingRuleDefinitionRequest;
import com.priceforge.pricing.infrastructure.in.http.dto.SimulatePriceRequest;
import com.priceforge.sharedkernel.ProductId;
import com.priceforge.sharedkernel.UserId;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class PriceCalculationHttpMapper {

    public CalculatePriceCommand toCommand(CalculatePriceRequest request) {
        return new CalculatePriceCommand(
            new ProductId(request.productId()),
            new UserId(request.userId()),
            request.userSegment(),
            request.quantity(),
            request.requestedAt() != null ? request.requestedAt() : Instant.now(),
            request.location(),
            request.currency(),
            request.zoneId() != null ? ZoneId.of(request.zoneId()) : ZoneId.of("UTC"));
    }

    public SimulatePriceCommand toCommand(SimulatePriceRequest request) {
        return new SimulatePriceCommand(
            new ProductId(request.productId()),
            new UserId(request.userId()),
            request.userSegment(),
            request.quantity(),
            request.requestedAt() != null ? request.requestedAt() : Instant.now(),
            request.location(),
            request.currency(),
            request.zoneId() != null ? ZoneId.of(request.zoneId()) : ZoneId.of("UTC"),
            request.hypotheticalRule() != null ? toHypotheticalRule(request.hypotheticalRule()) : null);
    }

    private HypotheticalPricingRule toHypotheticalRule(PricingRuleDefinitionRequest request) {
        return new HypotheticalPricingRule(
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

    public PriceQuoteResponse toResponse(CalculatePriceResult result) {
        return new PriceQuoteResponse(
            result.basePriceAmount(),
            result.finalPriceAmount(),
            result.currency(),
            result.appliedRules().stream()
                .map(rule -> new AppliedRuleResponse(
                    rule.ruleId().value(), rule.ruleType().name(), rule.description(),
                    rule.discountApplied().amount(), rule.reason()))
                .toList(),
            result.excludedRules().stream()
                .map(rule -> new ExcludedRuleResponse(rule.ruleId().value(), rule.reason().name()))
                .toList(),
            result.appliedPromotions().stream()
                .map(promo -> new AppliedPromotionResponse(
                    promo.sourceId(), promo.description(), promo.discountApplied().amount(), promo.reason()))
                .toList(),
            result.excludedPromotions().stream()
                .map(promo -> new ExcludedPromotionResponse(promo.sourceId(), promo.reason()))
                .toList(),
            result.calculatedAt(),
            result.simulated());
    }
}
