package com.priceforge.pricing.domain.service;

import com.priceforge.pricing.domain.model.AppliedRuleDetail;
import com.priceforge.pricing.domain.model.CombinationMode;
import com.priceforge.pricing.domain.model.Discount;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.ExclusionReason;
import com.priceforge.pricing.domain.model.ExcludedRuleDetail;
import com.priceforge.pricing.domain.model.PriceQuote;
import com.priceforge.pricing.domain.model.PricingContext;
import com.priceforge.pricing.domain.model.PricingRule;
import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.ProductId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * UC-06/UC-07: filters the candidate rules to the ones that actually apply (scope + condition),
 * resolves conflicts by priority and combination mode, and applies discounts to build a
 * {@link PriceQuote}. Pure domain logic — no framework, no persistence, no I/O — so it can be
 * reused unchanged by both the real calculation (UC-06) and the what-if simulator (UC-08).
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

        Map<Integer, List<PricingRule>> byPriorityDescending = applicable.stream()
            .collect(Collectors.groupingBy(
                PricingRule::priority,
                () -> new TreeMap<>(Comparator.reverseOrder()),
                Collectors.toList()));

        List<AppliedRuleDetail> appliedRules = new ArrayList<>();
        List<ExcludedRuleDetail> excludedRules = new ArrayList<>();
        Money currentPrice = basePrice;
        boolean exclusiveApplied = false;
        boolean priceFloorApplied = false;

        for (List<PricingRule> group : byPriorityDescending.values()) {
            if (exclusiveApplied) {
                excludeAll(group, ExclusionReason.EXCLUDED_BY_HIGHER_PRIORITY, excludedRules);
                continue;
            }

            List<PricingRule> exclusiveInGroup = group.stream()
                .filter(rule -> rule.combinationMode() == CombinationMode.EXCLUSIVE)
                .toList();

            if (exclusiveInGroup.isEmpty()) {
                // Same-priority STACKABLE rules must be order-independent (UC-07): compute every
                // discount in the group against the same starting price rather than chaining them
                // sequentially, which would make a PERCENTAGE + FIXED_AMOUNT mix order-sensitive.
                GroupOutcome outcome = applyStackableGroup(currentPrice, group);
                currentPrice = outcome.newPrice();
                priceFloorApplied |= outcome.floored();
                appliedRules.addAll(outcome.details());
                continue;
            }

            PricingRule winner = exclusiveInGroup.size() == 1
                ? exclusiveInGroup.get(0)
                : exclusiveInGroup.stream().min(Comparator.comparing(PricingRule::createdAt)).orElseThrow();

            for (PricingRule rule : group) {
                if (!rule.id().equals(winner.id())) {
                    excludedRules.add(new ExcludedRuleDetail(rule.id(), ExclusionReason.EXCLUDED_BY_TIEBREAK));
                }
            }

            ApplyOutcome outcome = applyDiscount(currentPrice, winner);
            currentPrice = outcome.newPrice();
            priceFloorApplied |= outcome.floored();
            appliedRules.add(outcome.detail());
            exclusiveApplied = true;
        }

        return new PriceQuote(
            basePrice, currentPrice, appliedRules, excludedRules, Instant.now(), context, priceFloorApplied, simulated);
    }

    private void excludeAll(List<PricingRule> rules, ExclusionReason reason, List<ExcludedRuleDetail> excludedRules) {
        for (PricingRule rule : rules) {
            excludedRules.add(new ExcludedRuleDetail(rule.id(), reason));
        }
    }

    private GroupOutcome applyStackableGroup(Money groupStartPrice, List<PricingRule> group) {
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

    private ApplyOutcome applyDiscount(Money currentPrice, PricingRule rule) {
        Money discountAmount = rule.discount().amountFor(currentPrice);
        Money nominalNewPrice = currentPrice.minus(discountAmount);

        boolean floored = nominalNewPrice.isNegative() || nominalNewPrice.isZero();
        Money newPrice = floored ? minimumPositivePrice(currentPrice.currency()) : nominalNewPrice;
        Money effectiveDiscount = currentPrice.minus(newPrice);

        AppliedRuleDetail detail = new AppliedRuleDetail(
            rule.id(), rule.type(), describeRule(rule), effectiveDiscount, matchReason(rule));

        return new ApplyOutcome(newPrice, floored, detail);
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

    private record ApplyOutcome(Money newPrice, boolean floored, AppliedRuleDetail detail) {
    }

    private record GroupOutcome(Money newPrice, boolean floored, List<AppliedRuleDetail> details) {
    }
}
