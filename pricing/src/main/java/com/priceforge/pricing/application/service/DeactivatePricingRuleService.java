package com.priceforge.pricing.application.service;

import com.priceforge.pricing.application.dto.DeactivatePricingRuleCommand;
import com.priceforge.pricing.application.dto.DeactivatePricingRuleResult;
import com.priceforge.pricing.application.port.in.DeactivatePricingRuleUseCase;
import com.priceforge.pricing.application.port.out.PricingRuleRepositoryPort;
import com.priceforge.pricing.domain.exception.PricingRuleNotFoundException;
import com.priceforge.pricing.domain.model.PricingRule;
import com.priceforge.pricing.domain.model.PricingRuleDeactivated;
import com.priceforge.sharedkernel.EventPublisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeactivatePricingRuleService implements DeactivatePricingRuleUseCase {

    private final PricingRuleRepositoryPort pricingRuleRepository;
    private final EventPublisher eventPublisher;

    public DeactivatePricingRuleService(
            PricingRuleRepositoryPort pricingRuleRepository,
            @Qualifier("pricingEventPublisher") EventPublisher eventPublisher) {
        this.pricingRuleRepository = pricingRuleRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('REVENUE_MANAGER')")
    public DeactivatePricingRuleResult execute(DeactivatePricingRuleCommand command) {
        PricingRule rule = pricingRuleRepository.findById(command.ruleId())
            .orElseThrow(() -> new PricingRuleNotFoundException(command.ruleId()));

        rule.deactivate();
        pricingRuleRepository.save(rule);
        eventPublisher.publish(PricingRuleDeactivated.of(rule, command.changedBy()));

        return new DeactivatePricingRuleResult(rule.id(), rule.status());
    }
}
