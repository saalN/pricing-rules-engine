package com.priceforge.pricing.application.service;

import com.priceforge.catalog.application.dto.GetProductQuery;
import com.priceforge.catalog.application.dto.GetProductResult;
import com.priceforge.catalog.application.port.in.GetProductUseCase;
import com.priceforge.pricing.application.dto.CalculatePriceResult;
import com.priceforge.pricing.application.dto.HypotheticalPricingRule;
import com.priceforge.pricing.application.dto.SimulatePriceCommand;
import com.priceforge.pricing.application.port.in.SimulatePriceUseCase;
import com.priceforge.pricing.application.port.out.PricingRuleRepositoryPort;
import com.priceforge.pricing.application.service.support.RuleDefinitionMapper;
import com.priceforge.pricing.domain.exception.CurrencyMismatchException;
import com.priceforge.pricing.domain.exception.ProductNotActiveException;
import com.priceforge.pricing.domain.exception.ProductNotFoundException;
import com.priceforge.pricing.domain.model.Discount;
import com.priceforge.pricing.domain.model.PriceQuote;
import com.priceforge.sharedkernel.PricingContext;
import com.priceforge.pricing.domain.model.PricingRule;
import com.priceforge.pricing.domain.model.RuleCondition;
import com.priceforge.pricing.domain.model.RuleScope;
import com.priceforge.pricing.domain.service.PriceCalculatorService;
import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.PricingRuleId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * UC-08: never persists, never publishes events, never touches the cache — the calculation is
 * purely in-memory, using the same {@link PriceCalculatorService} as the real UC-06 flow so the
 * simulation behaves identically to what a real calculation would do.
 */
@Service
public class SimulatePriceService implements SimulatePriceUseCase {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final GetProductUseCase getProductUseCase;
    private final PricingRuleRepositoryPort pricingRuleRepository;
    private final PriceCalculatorService calculator = new PriceCalculatorService();

    public SimulatePriceService(
            GetProductUseCase getProductUseCase, PricingRuleRepositoryPort pricingRuleRepository) {
        this.getProductUseCase = getProductUseCase;
        this.pricingRuleRepository = pricingRuleRepository;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or hasRole('REVENUE_MANAGER')")
    public CalculatePriceResult execute(SimulatePriceCommand command) {
        GetProductResult product = getProductUseCase.execute(new GetProductQuery(command.productId()))
            .orElseThrow(() -> new ProductNotFoundException(command.productId()));

        if (!ACTIVE_STATUS.equals(product.status())) {
            throw new ProductNotActiveException(command.productId());
        }
        if (!command.currency().equals(product.currency())) {
            throw new CurrencyMismatchException(command.currency(), product.currency());
        }

        PricingContext context = new PricingContext(
            command.productId(), command.userId(), command.userSegment(), command.quantity(),
            command.requestedAt(), command.location(), command.currency(), command.zoneId());

        List<PricingRule> rules = new ArrayList<>(pricingRuleRepository.findAllActive());
        if (command.hypotheticalRule() != null) {
            rules.add(toHypotheticalRule(command.hypotheticalRule()));
        }

        Money basePrice = Money.of(product.basePriceAmount(), product.currency());
        PriceQuote quote = calculator.calculate(
            basePrice, command.productId(), product.category(), rules, context, true);

        return CalculatePriceResult.from(quote);
    }

    private PricingRule toHypotheticalRule(HypotheticalPricingRule hypothetical) {
        RuleCondition condition = RuleDefinitionMapper.toCondition(
            hypothetical.type(), hypothetical.timeWindowStart(), hypothetical.timeWindowEnd(),
            hypothetical.segment(), hypothetical.minimumQuantity(), hypothetical.zone());
        RuleScope scope = RuleDefinitionMapper.toScope(hypothetical.scopeProductId(), hypothetical.scopeCategory());
        Discount discount = new Discount(hypothetical.discountType(), hypothetical.discountValue());

        return PricingRule.create(
            PricingRuleId.newId(), hypothetical.type(), condition, scope, discount,
            hypothetical.priority(), hypothetical.combinationMode(), Instant.now());
    }
}
