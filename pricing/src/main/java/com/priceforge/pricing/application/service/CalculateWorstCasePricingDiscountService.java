package com.priceforge.pricing.application.service;

import com.priceforge.pricing.application.dto.CalculateWorstCasePricingDiscountQuery;
import com.priceforge.pricing.application.dto.CalculateWorstCasePricingDiscountResult;
import com.priceforge.pricing.application.port.in.CalculateWorstCasePricingDiscountUseCase;
import com.priceforge.pricing.application.port.out.PricingRuleRepositoryPort;
import com.priceforge.pricing.domain.model.CategoryScope;
import com.priceforge.pricing.domain.model.GlobalScope;
import com.priceforge.pricing.domain.model.PricingRule;
import com.priceforge.pricing.domain.model.ProductScope;
import com.priceforge.pricing.domain.model.RuleScope;
import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.sharedkernel.Money;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * UC-13 step 2: a conservative-upper-bound scenario, not an exhaustive search of every rule
 * combination — {@code RuleCondition} (time window, segment, volume, geo) is deliberately ignored
 * here, since the worst case assumes every active rule's condition COULD be satisfied by some
 * request. {@code ProductScope} rules are excluded: this query only knows the cheapest active
 * price in a category (or globally), never a concrete product id, so there is no way to tell
 * whether a specific-product rule would apply to it — the same limitation
 * {@code CreatePricingRuleService}'s own UC-05 zero-price check accepts for category/global scopes.
 */
@Service
public class CalculateWorstCasePricingDiscountService implements CalculateWorstCasePricingDiscountUseCase {

    private final PricingRuleRepositoryPort pricingRuleRepository;

    public CalculateWorstCasePricingDiscountService(PricingRuleRepositoryPort pricingRuleRepository) {
        this.pricingRuleRepository = pricingRuleRepository;
    }

    @Override
    public CalculateWorstCasePricingDiscountResult execute(CalculateWorstCasePricingDiscountQuery query) {
        Money basePrice = Money.of(query.basePriceAmount(), query.currency());
        Money zero = Money.of(BigDecimal.ZERO, query.currency());

        List<PricingRule> applicable = pricingRuleRepository.findAllActive().stream()
            .filter(rule -> appliesToScope(rule.scope(), query.category()))
            .toList();

        Money stackableSum = applicable.stream()
            .filter(rule -> rule.combinationMode() == CombinationMode.STACKABLE)
            .map(rule -> rule.discount().amountFor(basePrice))
            .reduce(zero, Money::plus);

        Money exclusiveMax = applicable.stream()
            .filter(rule -> rule.combinationMode() == CombinationMode.EXCLUSIVE)
            .map(rule -> rule.discount().amountFor(basePrice))
            .max(Comparator.comparing(Money::amount))
            .orElse(zero);

        BigDecimal worstCase = stackableSum.amount().max(exclusiveMax.amount());
        return new CalculateWorstCasePricingDiscountResult(worstCase);
    }

    /** {@code category == null} means the promotion being validated is global: every category could be the winner. */
    private boolean appliesToScope(RuleScope scope, String category) {
        return switch (scope) {
            case GlobalScope ignored -> true;
            case CategoryScope categoryScope -> category == null || categoryScope.category().equals(category);
            case ProductScope ignored -> false;
        };
    }
}
