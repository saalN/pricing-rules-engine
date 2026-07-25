package com.priceforge.pricing.application.service;

import com.priceforge.catalog.application.dto.GetProductQuery;
import com.priceforge.catalog.application.dto.GetProductResult;
import com.priceforge.catalog.application.port.in.GetProductUseCase;
import com.priceforge.pricing.application.dto.CalculatePriceCommand;
import com.priceforge.pricing.application.dto.CalculatePriceResult;
import com.priceforge.pricing.application.port.in.CalculatePriceUseCase;
import com.priceforge.pricing.application.port.out.PriceQuoteCachePort;
import com.priceforge.pricing.application.port.out.PricingRuleRepositoryPort;
import com.priceforge.pricing.domain.exception.CurrencyMismatchException;
import com.priceforge.pricing.domain.exception.ProductNotActiveException;
import com.priceforge.pricing.domain.exception.ProductNotFoundException;
import com.priceforge.pricing.domain.model.ConflictReason;
import com.priceforge.sharedkernel.ConflictExclusionReason;
import com.priceforge.pricing.domain.model.PriceCalculated;
import com.priceforge.pricing.domain.model.PriceQuote;
import com.priceforge.pricing.domain.model.PricingContext;
import com.priceforge.pricing.domain.model.PricingRule;
import com.priceforge.pricing.domain.model.RuleConflictDetected;
import com.priceforge.pricing.domain.service.PriceCalculatorService;
import com.priceforge.sharedkernel.EventPublisher;
import com.priceforge.sharedkernel.Money;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CalculatePriceService implements CalculatePriceUseCase {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final GetProductUseCase getProductUseCase;
    private final PricingRuleRepositoryPort pricingRuleRepository;
    private final PriceQuoteCachePort cache;
    private final EventPublisher eventPublisher;
    private final PriceCalculatorService calculator = new PriceCalculatorService();

    public CalculatePriceService(
            GetProductUseCase getProductUseCase,
            PricingRuleRepositoryPort pricingRuleRepository,
            PriceQuoteCachePort cache,
            @Qualifier("pricingEventPublisher") EventPublisher eventPublisher) {
        this.getProductUseCase = getProductUseCase;
        this.pricingRuleRepository = pricingRuleRepository;
        this.cache = cache;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public CalculatePriceResult execute(CalculatePriceCommand command) {
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

        var cached = cache.get(command.productId(), context.cacheKey());
        if (cached.isPresent()) {
            return CalculatePriceResult.from(cached.get());
        }

        List<PricingRule> activeRules = pricingRuleRepository.findAllActive();
        Money basePrice = Money.of(product.basePriceAmount(), product.currency());

        PriceQuote quote = calculator.calculate(
            basePrice, command.productId(), product.category(), activeRules, context, false);

        eventPublisher.publish(PriceCalculated.of(quote));
        publishConflictsIfAny(quote);
        cache.put(command.productId(), context.cacheKey(), quote);

        return CalculatePriceResult.from(quote);
    }

    private void publishConflictsIfAny(PriceQuote quote) {
        List<String> appliedRuleIds = quote.appliedRules().stream().map(rule -> rule.ruleId().toString()).toList();
        if (quote.priceFloorApplied()) {
            eventPublisher.publish(RuleConflictDetected.of(
                quote.contextSnapshot().productId(), ConflictReason.PRICE_FLOOR_APPLIED, appliedRuleIds));
        }

        List<String> tiebreakRuleIds = quote.excludedRules().stream()
            .filter(excluded -> excluded.reason() == ConflictExclusionReason.EXCLUDED_BY_TIEBREAK)
            .map(excluded -> excluded.ruleId().toString())
            .toList();
        if (!tiebreakRuleIds.isEmpty()) {
            eventPublisher.publish(RuleConflictDetected.of(
                quote.contextSnapshot().productId(), ConflictReason.TIEBREAK, tiebreakRuleIds));
        }
    }
}
