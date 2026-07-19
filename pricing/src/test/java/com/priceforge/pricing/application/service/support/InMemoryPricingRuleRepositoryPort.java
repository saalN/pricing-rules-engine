package com.priceforge.pricing.application.service.support;

import com.priceforge.pricing.application.port.out.PricingRuleRepositoryPort;
import com.priceforge.pricing.domain.model.PricingRule;
import com.priceforge.pricing.domain.model.PricingRuleStatus;
import com.priceforge.sharedkernel.PricingRuleId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryPricingRuleRepositoryPort implements PricingRuleRepositoryPort {

    private final Map<PricingRuleId, PricingRule> rulesById = new HashMap<>();

    @Override
    public PricingRule save(PricingRule rule) {
        rulesById.put(rule.id(), rule);
        return rule;
    }

    @Override
    public Optional<PricingRule> findById(PricingRuleId id) {
        return Optional.ofNullable(rulesById.get(id));
    }

    @Override
    public List<PricingRule> findAllActive() {
        return rulesById.values().stream()
            .filter(rule -> rule.status() == PricingRuleStatus.ACTIVE)
            .toList();
    }
}
