package com.priceforge.promotions.application.service;

import com.priceforge.catalog.application.dto.FindCheapestActiveBasePriceQuery;
import com.priceforge.catalog.application.port.in.FindCheapestActiveBasePriceUseCase;
import com.priceforge.pricing.application.dto.CalculateWorstCasePricingDiscountQuery;
import com.priceforge.pricing.application.port.in.CalculateWorstCasePricingDiscountUseCase;
import com.priceforge.promotions.application.dto.CreatePromotionCommand;
import com.priceforge.promotions.application.dto.CreatePromotionResult;
import com.priceforge.promotions.application.port.in.CreatePromotionUseCase;
import com.priceforge.promotions.application.port.out.PromotionRepositoryPort;
import com.priceforge.promotions.domain.exception.InvalidPromotionDiscountException;
import com.priceforge.promotions.domain.model.Discount;
import com.priceforge.promotions.domain.model.EligibilityCriteria;
import com.priceforge.promotions.domain.model.Promotion;
import com.priceforge.promotions.domain.model.PromotionCreated;
import com.priceforge.sharedkernel.EventPublisher;
import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.PromotionId;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatePromotionService implements CreatePromotionUseCase {

    private final PromotionRepositoryPort promotionRepository;
    private final FindCheapestActiveBasePriceUseCase findCheapestActiveBasePriceUseCase;
    private final CalculateWorstCasePricingDiscountUseCase calculateWorstCasePricingDiscountUseCase;
    private final EventPublisher eventPublisher;

    public CreatePromotionService(
            PromotionRepositoryPort promotionRepository,
            FindCheapestActiveBasePriceUseCase findCheapestActiveBasePriceUseCase,
            CalculateWorstCasePricingDiscountUseCase calculateWorstCasePricingDiscountUseCase,
            @Qualifier("promotionsEventPublisher") EventPublisher eventPublisher) {
        this.promotionRepository = promotionRepository;
        this.findCheapestActiveBasePriceUseCase = findCheapestActiveBasePriceUseCase;
        this.calculateWorstCasePricingDiscountUseCase = calculateWorstCasePricingDiscountUseCase;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING')")
    public CreatePromotionResult execute(CreatePromotionCommand command) {
        EligibilityCriteria eligibility = new EligibilityCriteria(
            command.segment(), command.category(), command.minimumPurchaseAmount());
        Discount discount = new Discount(command.discountType(), command.discountValue());

        rejectIfCouldBreachZero(command.category(), command.currency(), discount);

        Promotion promotion = Promotion.create(
            PromotionId.newId(), command.name(), command.description(), command.validFrom(), command.validUntil(),
            command.globalUsageLimit(), command.perUserUsageLimit(), eligibility, discount,
            command.priority(), command.combinationMode(), Instant.now());

        promotionRepository.save(promotion);
        eventPublisher.publish(PromotionCreated.of(promotion));

        return new CreatePromotionResult(promotion.id());
    }

    /**
     * UC-13: a conservative-upper-bound scenario, not an exhaustive search — the cheapest active
     * product in scope, discounted by the worst case of (all applicable STACKABLE pricing rules
     * summed) vs (the single largest applicable EXCLUSIVE pricing rule), plus this promotion's own
     * discount. If that combination could drive the price to zero or below, the promotion is
     * rejected preventively rather than corrected in production later.
     */
    private void rejectIfCouldBreachZero(String category, String currency, Discount discount) {
        BigDecimal cheapestBasePriceAmount = findCheapestActiveBasePriceUseCase
            .execute(new FindCheapestActiveBasePriceQuery(category))
            .cheapestBasePriceAmount();
        if (cheapestBasePriceAmount == null) {
            // No active product in scope yet — nothing to validate against (best-effort, same
            // philosophy as Pricing's own UC-05 zero-price check).
            return;
        }

        BigDecimal worstCasePricingDiscount = calculateWorstCasePricingDiscountUseCase
            .execute(new CalculateWorstCasePricingDiscountQuery(cheapestBasePriceAmount, currency, category))
            .worstCaseDiscountAmount();

        Money cheapestPrice = Money.of(cheapestBasePriceAmount, currency);
        Money promotionDiscount = discount.amountFor(cheapestPrice);
        Money totalWorstCaseDiscount = Money.of(worstCasePricingDiscount, currency).plus(promotionDiscount);

        if (cheapestPrice.minus(totalWorstCaseDiscount).amount().signum() <= 0) {
            throw new InvalidPromotionDiscountException(
                "This promotion, combined with the worst-case active pricing rules, could drive the price of "
                    + "the cheapest active product in scope (" + cheapestPrice.roundedToCurrency().amount().toPlainString()
                    + " " + currency
                    + ") to zero or below");
        }
    }
}
