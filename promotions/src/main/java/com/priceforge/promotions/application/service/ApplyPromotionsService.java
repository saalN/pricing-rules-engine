package com.priceforge.promotions.application.service;

import com.priceforge.sharedkernel.AppliedPromotionDetail;
import com.priceforge.sharedkernel.ExcludedPromotionDetail;
import com.priceforge.sharedkernel.PricingContext;
import com.priceforge.sharedkernel.PromotionApplicationOutcome;
import com.priceforge.promotions.application.dto.ApplyPromotionsCommand;
import com.priceforge.promotions.application.port.in.ApplyPromotionsUseCase;
import com.priceforge.promotions.application.port.out.PromotionRepositoryPort;
import com.priceforge.promotions.domain.model.Discount;
import com.priceforge.promotions.domain.model.DiscountType;
import com.priceforge.promotions.domain.model.Promotion;
import com.priceforge.promotions.domain.model.PromotionApplied;
import com.priceforge.promotions.domain.model.PromotionExhausted;
import com.priceforge.promotions.domain.model.UsageRecordingOutcome;
import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.sharedkernel.ConflictResolution;
import com.priceforge.sharedkernel.ConflictResolver;
import com.priceforge.sharedkernel.EventPublisher;
import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.PromotionId;
import com.priceforge.sharedkernel.ResolvedGroup;
import com.priceforge.sharedkernel.UserId;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-11, reached by Pricing through its {@code PromotionApplierPort} extension point (see
 * {@code PricingPromotionApplierAdapter}). Mirrors {@code PriceCalculatorService}'s own shape: eligibility filter, shared
 * {@link ConflictResolver}, then apply groups in priority order, each group computed against a
 * common starting price. The one addition UC-07/UC-06 doesn't need: an atomic usage-limit
 * check-and-increment per promotion before its discount is allowed to count, since limits must
 * never be bypassed by a losing race.
 */
@Service
public class ApplyPromotionsService implements ApplyPromotionsUseCase {

    private final PromotionRepositoryPort promotionRepository;
    private final EventPublisher eventPublisher;

    public ApplyPromotionsService(
            PromotionRepositoryPort promotionRepository,
            @Qualifier("promotionsEventPublisher") EventPublisher eventPublisher) {
        this.promotionRepository = promotionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public PromotionApplicationOutcome execute(ApplyPromotionsCommand command) {
        Money currentPrice = command.currentPrice();
        PricingContext context = command.context();
        String category = command.category();
        List<Promotion> eligible = promotionRepository.findAllActive().stream()
            .filter(promotion -> promotion.isEligible(context.userSegment(), category, currentPrice, context.requestedAt()))
            .toList();

        ConflictResolution<Promotion, PromotionId> resolution = ConflictResolver.resolve(eligible);

        List<AppliedPromotionDetail> applied = new ArrayList<>();
        List<ExcludedPromotionDetail> excluded = new ArrayList<>();
        Money price = currentPrice;
        boolean priceFloorApplied = false;

        for (ResolvedGroup<Promotion> group : resolution.groupsToApply()) {
            GroupOutcome outcome = applyGroup(price, group.items(), context.userId(), applied, excluded);
            price = outcome.newPrice();
            priceFloorApplied |= outcome.floored();
        }

        for (var excludedItem : resolution.excluded()) {
            excluded.add(new ExcludedPromotionDetail(excludedItem.id().toString(), excludedItem.reason().name()));
        }

        return new PromotionApplicationOutcome(price, applied, excluded, priceFloorApplied);
    }

    private GroupOutcome applyGroup(
            Money groupStartPrice,
            List<Promotion> group,
            UserId userId,
            List<AppliedPromotionDetail> appliedOut,
            List<ExcludedPromotionDetail> excludedOut) {
        Money totalDiscount = Money.of(BigDecimal.ZERO, groupStartPrice.currency().getCurrencyCode());

        for (Promotion promotion : group) {
            UsageRecordingOutcome usageOutcome = promotionRepository.recordUsageIfWithinLimits(promotion.id(), userId);

            if (usageOutcome == UsageRecordingOutcome.GLOBAL_LIMIT_REACHED
                    || usageOutcome == UsageRecordingOutcome.PER_USER_LIMIT_REACHED) {
                excludedOut.add(new ExcludedPromotionDetail(promotion.id().toString(), "LIMIT_REACHED"));
                continue;
            }

            Money discountAmount = promotion.discount().amountFor(groupStartPrice);
            totalDiscount = totalDiscount.plus(discountAmount);
            appliedOut.add(new AppliedPromotionDetail(
                promotion.id().toString(), describe(promotion), discountAmount, "eligible"));
            eventPublisher.publish(PromotionApplied.of(promotion.id(), userId, discountAmount));

            if (usageOutcome == UsageRecordingOutcome.APPLIED_AND_NOW_EXHAUSTED) {
                eventPublisher.publish(PromotionExhausted.of(promotion));
            }
        }

        Money nominalNewPrice = groupStartPrice.minus(totalDiscount);
        boolean floored = nominalNewPrice.isNegative() || nominalNewPrice.isZero();
        Money newPrice = floored ? minimumPositivePrice(groupStartPrice.currency()) : nominalNewPrice;

        return new GroupOutcome(newPrice, floored);
    }

    private String describe(Promotion promotion) {
        Discount discount = promotion.discount();
        String discountText = discount.type() == DiscountType.PERCENTAGE
            ? discount.value() + "%"
            : discount.value().toString();
        return promotion.name() + " (" + discount.type() + " " + discountText + ")";
    }

    private Money minimumPositivePrice(Currency currency) {
        int fractionDigits = Math.max(currency.getDefaultFractionDigits(), 0);
        return new Money(BigDecimal.ONE.movePointLeft(fractionDigits), currency);
    }

    private record GroupOutcome(Money newPrice, boolean floored) {
    }
}
