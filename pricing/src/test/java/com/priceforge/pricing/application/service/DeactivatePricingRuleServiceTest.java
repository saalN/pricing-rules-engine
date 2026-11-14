package com.priceforge.pricing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.pricing.application.dto.DeactivatePricingRuleCommand;
import com.priceforge.pricing.application.dto.DeactivatePricingRuleResult;
import com.priceforge.pricing.application.service.support.InMemoryPricingRuleRepositoryPort;
import com.priceforge.pricing.application.service.support.RecordingEventPublisher;
import com.priceforge.pricing.domain.exception.PricingRuleNotFoundException;
import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.sharedkernel.CustomerSegment;
import com.priceforge.pricing.domain.model.Discount;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.GlobalScope;
import com.priceforge.pricing.domain.model.PricingRule;
import com.priceforge.pricing.domain.model.PricingRuleDeactivated;
import com.priceforge.pricing.domain.model.PricingRuleStatus;
import com.priceforge.pricing.domain.model.PricingRuleType;
import com.priceforge.pricing.domain.model.SegmentCondition;
import com.priceforge.sharedkernel.PricingRuleId;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DeactivatePricingRuleServiceTest {

    private final InMemoryPricingRuleRepositoryPort pricingRuleRepository = new InMemoryPricingRuleRepositoryPort();
    private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
    private final DeactivatePricingRuleService service =
        new DeactivatePricingRuleService(pricingRuleRepository, eventPublisher);

    @Test
    void deactivatesTheRuleAndPublishesPricingRuleDeactivated() {
        PricingRule rule = PricingRule.create(
            PricingRuleId.newId(), PricingRuleType.SEGMENT_BASED, new SegmentCondition(CustomerSegment.VIP),
            new GlobalScope(), new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(10)),
            10, CombinationMode.STACKABLE, Instant.now());
        pricingRuleRepository.save(rule);

        DeactivatePricingRuleResult result =
            service.execute(new DeactivatePricingRuleCommand(rule.id(), "admin@priceforge.com"));

        assertThat(result.status()).isEqualTo(PricingRuleStatus.INACTIVE);
        assertThat(eventPublisher.publishedEvents()).anyMatch(PricingRuleDeactivated.class::isInstance);
    }

    @Test
    void rejectsDeactivatingARuleThatDoesNotExist() {
        assertThatThrownBy(() -> service.execute(
                new DeactivatePricingRuleCommand(PricingRuleId.newId(), "admin@priceforge.com")))
            .isInstanceOf(PricingRuleNotFoundException.class);
    }
}
