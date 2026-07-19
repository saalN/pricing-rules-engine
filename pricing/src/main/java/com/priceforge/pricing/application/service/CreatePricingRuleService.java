package com.priceforge.pricing.application.service;

import com.priceforge.catalog.application.dto.FindCheapestActiveBasePriceQuery;
import com.priceforge.catalog.application.dto.GetProductQuery;
import com.priceforge.catalog.application.port.in.FindCheapestActiveBasePriceUseCase;
import com.priceforge.catalog.application.port.in.GetProductUseCase;
import com.priceforge.pricing.application.dto.CreatePricingRuleCommand;
import com.priceforge.pricing.application.dto.CreatePricingRuleResult;
import com.priceforge.pricing.application.port.in.CreatePricingRuleUseCase;
import com.priceforge.pricing.application.port.out.PricingRuleRepositoryPort;
import com.priceforge.pricing.application.service.support.RuleDefinitionMapper;
import com.priceforge.pricing.domain.exception.InvalidDiscountException;
import com.priceforge.pricing.domain.model.CategoryScope;
import com.priceforge.pricing.domain.model.Discount;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.GlobalScope;
import com.priceforge.pricing.domain.model.PricingRule;
import com.priceforge.pricing.domain.model.PricingRuleCreated;
import com.priceforge.pricing.domain.model.ProductScope;
import com.priceforge.pricing.domain.model.RuleCondition;
import com.priceforge.pricing.domain.model.RuleScope;
import com.priceforge.sharedkernel.EventPublisher;
import com.priceforge.sharedkernel.PricingRuleId;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatePricingRuleService implements CreatePricingRuleUseCase {

    private final PricingRuleRepositoryPort pricingRuleRepository;
    private final GetProductUseCase getProductUseCase;
    private final FindCheapestActiveBasePriceUseCase findCheapestActiveBasePriceUseCase;
    private final EventPublisher eventPublisher;

    public CreatePricingRuleService(
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
    public CreatePricingRuleResult execute(CreatePricingRuleCommand command) {
        RuleCondition condition = RuleDefinitionMapper.toCondition(
            command.type(), command.timeWindowStart(), command.timeWindowEnd(),
            command.segment(), command.minimumQuantity(), command.zone());
        RuleScope scope = RuleDefinitionMapper.toScope(command.scopeProductId(), command.scopeCategory());
        Discount discount = new Discount(command.discountType(), command.discountValue());

        rejectIfDiscountCouldBreachZero(discount, scope);

        PricingRule rule = PricingRule.create(
            PricingRuleId.newId(), command.type(), condition, scope, discount,
            command.priority(), command.combinationMode(), Instant.now());

        pricingRuleRepository.save(rule);
        eventPublisher.publish(PricingRuleCreated.of(rule));

        return new CreatePricingRuleResult(rule.id());
    }

    /**
     * UC-05: "the discount can never drive the price to zero or negative in any scenario."
     * PERCENTAGE >= 100 is already rejected by {@link Discount}'s own invariant; FIXED_AMOUNT can
     * only be checked against the cheapest active product actually in scope, which requires
     * querying Catalog (allowed: a context may depend on another context's published ports).
     */
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
            // No active product in scope yet to validate against — same best-effort philosophy
            // as UC-13's promotion validation, not a reason to block rule creation.
            return;
        }

        if (worstCaseBasePrice.subtract(discount.value()).signum() <= 0) {
            throw new InvalidDiscountException(
                "A fixed discount of " + discount.value() + " would drive the price of the cheapest "
                    + "active product in scope (" + worstCaseBasePrice + ") to zero or below");
        }
    }
}
