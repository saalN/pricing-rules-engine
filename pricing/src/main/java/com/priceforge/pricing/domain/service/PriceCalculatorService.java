package com.priceforge.pricing.domain.service;

import com.priceforge.pricing.domain.model.AppliedRuleDetail;
import com.priceforge.pricing.domain.model.Discount;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.ExcludedRuleDetail;
import com.priceforge.pricing.domain.model.PriceQuote;
import com.priceforge.pricing.domain.model.PricingContext;
import com.priceforge.pricing.domain.model.PricingRule;
import com.priceforge.sharedkernel.ConflictResolution;
import com.priceforge.sharedkernel.ConflictResolver;
import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.PricingRuleId;
import com.priceforge.sharedkernel.ProductId;
import com.priceforge.sharedkernel.ResolvedGroup;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

/**
 * UC-06/UC-07: filters the candidate rules to the ones that actually apply (scope + condition),
 * resolves conflicts via shared-kernel's generic {@link ConflictResolver}, and applies discounts
 * to build a {@link PriceQuote}. Pure domain logic — no framework, no persistence, no I/O — so it
 * can be reused unchanged by both the real calculation (UC-06) and the what-if simulator (UC-08).
 */
public final class PriceCalculatorService {

    public PriceQuote calculate(
            Money basePrice,
            ProductId productId,
            String category,
            List<PricingRule> activeRules,
            PricingContext context,
            boolean simulated) {

        List<PricingRule> applicable = activeRules.stream()
            .filter(rule -> rule.scope().appliesTo(productId, category))
            .filter(rule -> rule.condition().matches(context))
            .toList();

        ConflictResolution<PricingRule, PricingRuleId> resolution = ConflictResolver.resolve(applicable);

        List<AppliedRuleDetail> appliedRules = new ArrayList<>();
        List<ExcludedRuleDetail> excludedRules = new ArrayList<>();
        Money currentPrice = basePrice;
        boolean priceFloorApplied = false;

        for (ResolvedGroup<PricingRule> group : resolution.groupsToApply()) {
            // A group with more than one item is always a same-priority STACKABLE tie: every
            // discount in it is computed against the same starting price rather than chained
            // sequentially, which would make a PERCENTAGE + FIXED_AMOUNT mix order-sensitive.
            GroupOutcome outcome = applyGroup(currentPrice, group.items());
            currentPrice = outcome.newPrice();
            priceFloorApplied |= outcome.floored();
            appliedRules.addAll(outcome.details());
        }

        for (var excluded : resolution.excluded()) {
            excludedRules.add(new ExcludedRuleDetail(excluded.id(), excluded.reason()));
        }

        return new PriceQuote(
            basePrice, currentPrice, appliedRules, excludedRules, Instant.now(), context, priceFloorApplied, simulated);
    }

    private GroupOutcome applyGroup(Money groupStartPrice, List<PricingRule> group) {
        Money totalDiscount = Money.of(BigDecimal.ZERO, groupStartPrice.currency().getCurrencyCode());
        List<AppliedRuleDetail> details = new ArrayList<>();

        for (PricingRule rule : group) {
            Money discountAmount = rule.discount().amountFor(groupStartPrice);
            totalDiscount = totalDiscount.plus(discountAmount);
            details.add(new AppliedRuleDetail(
                rule.id(), rule.type(), describeRule(rule), discountAmount, matchReason(rule)));
        }

        Money nominalNewPrice = groupStartPrice.minus(totalDiscount);
        boolean floored = nominalNewPrice.isNegative() || nominalNewPrice.isZero();
        Money newPrice = floored ? minimumPositivePrice(groupStartPrice.currency()) : nominalNewPrice;

        return new GroupOutcome(newPrice, floored, details);
    }

    private String matchReason(PricingRule rule) {
        return "Matches because " + rule.condition().describe();
    }

    private String describeRule(PricingRule rule) {
        Discount discount = rule.discount();
        String discountText = discount.type() == DiscountType.PERCENTAGE
            ? discount.value() + "%"
            : discount.value().toString();
        return rule.type() + " rule (" + discount.type() + " " + discountText + ")";
    }

    private Money minimumPositivePrice(Currency currency) {
        int fractionDigits = Math.max(currency.getDefaultFractionDigits(), 0);
        return new Money(BigDecimal.ONE.movePointLeft(fractionDigits), currency);
    }

    private record GroupOutcome(Money newPrice, boolean floored, List<AppliedRuleDetail> details) {
    }
}
