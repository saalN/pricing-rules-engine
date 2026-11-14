package com.priceforge.pricing.application.service;

import com.priceforge.catalog.application.dto.GetProductQuery;
import com.priceforge.catalog.application.dto.GetProductResult;
import com.priceforge.catalog.application.port.in.GetProductUseCase;
import com.priceforge.pricing.application.dto.ReplayPriceCalculationCommand;
import com.priceforge.pricing.application.dto.ReplayPriceCalculationResult;
import com.priceforge.pricing.application.port.in.ReplayPriceCalculationUseCase;
import com.priceforge.pricing.application.port.out.PricingRuleRepositoryPort;
import com.priceforge.pricing.domain.exception.CurrencyMismatchException;
import com.priceforge.pricing.domain.exception.ProductNotFoundException;
import com.priceforge.pricing.domain.model.CustomerSegment;
import com.priceforge.pricing.domain.model.PriceQuote;
import com.priceforge.pricing.domain.model.PricingContext;
import com.priceforge.pricing.domain.service.PriceCalculatorService;
import com.priceforge.sharedkernel.Money;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * UC-14 replay: like {@link SimulatePriceService} it never persists, publishes or caches, and uses
 * the same {@link PriceCalculatorService}, but with no hypothetical rule and open to the AUDITOR
 * role (which Simulate rightly is not). It runs the pricing rules only — promotions are not
 * re-applied, since their usage counters must not be touched by a read-only audit check — and
 * unlike a live calculation it does not require the product to still be ACTIVE: an audited
 * calculation may well be for a product deactivated since.
 */
@Service
public class ReplayPriceCalculationService implements ReplayPriceCalculationUseCase {

    private final GetProductUseCase getProductUseCase;
    private final PricingRuleRepositoryPort pricingRuleRepository;
    private final PriceCalculatorService calculator = new PriceCalculatorService();

    public ReplayPriceCalculationService(
            GetProductUseCase getProductUseCase, PricingRuleRepositoryPort pricingRuleRepository) {
        this.getProductUseCase = getProductUseCase;
        this.pricingRuleRepository = pricingRuleRepository;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ReplayPriceCalculationResult execute(ReplayPriceCalculationCommand command) {
        GetProductResult product = getProductUseCase.execute(new GetProductQuery(command.productId()))
            .orElseThrow(() -> new ProductNotFoundException(command.productId()));

        if (!command.currency().equals(product.currency())) {
            throw new CurrencyMismatchException(command.currency(), product.currency());
        }

        PricingContext context = new PricingContext(
            command.productId(), command.userId(), CustomerSegment.valueOf(command.userSegment()),
            command.quantity(), command.requestedAt(), command.location(), command.currency(), command.zoneId());

        PriceQuote quote = calculator.calculate(
            Money.of(product.basePriceAmount(), product.currency()), command.productId(), product.category(),
            pricingRuleRepository.findAllActive(), context, true);

        return new ReplayPriceCalculationResult(
            quote.basePrice().amount(), quote.finalPrice().amount(),
            quote.finalPrice().currency().getCurrencyCode());
    }
}
