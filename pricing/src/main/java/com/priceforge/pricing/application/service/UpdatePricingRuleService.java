package com.priceforge.pricing.application.service;

import com.priceforge.catalog.application.dto.FindCheapestActiveBasePriceQuery;
import com.priceforge.catalog.application.dto.GetProductQuery;
import com.priceforge.catalog.application.port.in.FindCheapestActiveBasePriceUseCase;
import com.priceforge.catalog.application.port.in.GetProductUseCase;
import com.priceforge.pricing.application.dto.UpdatePricingRuleCommand;
import com.priceforge.pricing.application.dto.UpdatePricingRuleResult;
import com.priceforge.pricing.application.port.in.UpdatePricingRuleUseCase;
import com.priceforge.pricing.application.port.out.PricingRuleRepositoryPort;
import com.priceforge.pricing.application.service.support.RuleDefinitionMapper;
import com.priceforge.pricing.domain.exception.InvalidDiscountException;
import com.priceforge.pricing.domain.exception.PricingRuleNotFoundException;
import com.priceforge.pricing.domain.model.CategoryScope;
import com.priceforge.pricing.domain.model.Discount;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.GlobalScope;
import com.priceforge.pricing.domain.model.PricingRule;
import com.priceforge.pricing.domain.model.PricingRuleUpdated;
import com.priceforge.pricing.domain.model.ProductScope;
import com.priceforge.pricing.domain.model.RuleCondition;
import com.priceforge.pricing.domain.model.RuleScope;
import com.priceforge.sharedkernel.EventPublisher;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdatePricingRuleService implements UpdatePricingRuleUseCase {

    private final PricingRuleRepositoryPort pricingRuleRepository;
    private final GetProductUseCase getProductUseCase;
    private final FindCheapestActiveBasePriceUseCase findCheapestActiveBasePriceUseCase;
    private final EventPublisher eventPublisher;

    public UpdatePricingRuleService(
            PricingRuleRepositoryPort pricingRuleRepository,
            GetProductUseCase getProductUseCase,
            FindCheapestActiveBasePriceUseCase findCheapestActiveBasePriceUseCase,
            @Qualifier("pricingEventPublisher") EventPublisher eventPublisher) {
        this.pricingRuleRepository = pricingRuleRepository;
        this.getProductUseCase = getProductUseCase;
        this.findCheapestActiveBasePriceUseCase = findCheapestActiveBasePriceUseCase;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('REVENUE_MANAGER')")
    public UpdatePricingRuleResult execute(UpdatePricingRuleCommand command) {
        PricingRule rule = pricingRuleRepository.findById(command.ruleId())
            .orElseThrow(() -> new PricingRuleNotFoundException(command.ruleId()));

        String previousState = rule.describeState();

        RuleCondition newCondition = RuleDefinitionMapper.toCondition(
            rule.type(), command.timeWindowStart(), command.timeWindowEnd(),
            command.segment(), command.minimumQuantity(), command.zone());
        Discount newDiscount = new Discount(command.discountType(), command.discountValue());

        rejectIfDiscountCouldBreachZero(newDiscount, rule.scope());

        rule.update(newCondition, newDiscount, command.priority(), command.combinationMode());
        pricingRuleRepository.save(rule);
        eventPublisher.publish(PricingRuleUpdated.of(previousState, rule, command.changedBy()));

        return new UpdatePricingRuleResult(rule.id(), previousState, rule.describeState());
    }

    private void rejectIfDiscountCouldBreachZero(Discount discount, RuleScope scope) {
        if (discount.type() != DiscountType.FIXED_AMOUNT) {
            return;
        }

        BigDecimal worstCaseBasePrice = switch (scope) {
            case ProductScope productScope -> getProductUseCase
                .execute(new GetProductQuery(productScope.productId()))
                .map(product -> product.basePriceAmount())
                .orElse(null);
            case CategoryScope categoryScope -> findCheapestActiveBasePriceUseCase
                .execute(new FindCheapestActiveBasePriceQuery(categoryScope.category()))
                .cheapestBasePriceAmount();
            case GlobalScope globalScope -> findCheapestActiveBasePriceUseCase
                .execute(new FindCheapestActiveBasePriceQuery(null))
                .cheapestBasePriceAmount();
        };

        if (worstCaseBasePrice == null) {
            return;
        }

        if (worstCaseBasePrice.subtract(discount.value()).signum() <= 0) {
            throw new InvalidDiscountException(
                "A fixed discount of " + discount.value() + " would drive the price of the cheapest "
                    + "active product in scope (" + worstCaseBasePrice + ") to zero or below");
        }
    }
}
