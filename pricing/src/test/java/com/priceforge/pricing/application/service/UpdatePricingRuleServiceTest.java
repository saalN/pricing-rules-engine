package com.priceforge.pricing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.pricing.application.dto.UpdatePricingRuleCommand;
import com.priceforge.pricing.application.dto.UpdatePricingRuleResult;
import com.priceforge.pricing.application.service.support.FakeFindCheapestActiveBasePriceUseCase;
import com.priceforge.pricing.application.service.support.FakeGetProductUseCase;
import com.priceforge.pricing.application.service.support.InMemoryPricingRuleRepositoryPort;
import com.priceforge.pricing.application.service.support.RecordingEventPublisher;
import com.priceforge.pricing.domain.exception.PricingRuleNotFoundException;
import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.pricing.domain.model.CustomerSegment;
import com.priceforge.pricing.domain.model.Discount;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.GlobalScope;
import com.priceforge.pricing.domain.model.PricingRule;
import com.priceforge.pricing.domain.model.PricingRuleType;
import com.priceforge.pricing.domain.model.PricingRuleUpdated;
import com.priceforge.pricing.domain.model.SegmentCondition;
import com.priceforge.sharedkernel.PricingRuleId;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UpdatePricingRuleServiceTest {

    private final InMemoryPricingRuleRepositoryPort pricingRuleRepository = new InMemoryPricingRuleRepositoryPort();
    private final FakeGetProductUseCase getProductUseCase = new FakeGetProductUseCase();
    private final FakeFindCheapestActiveBasePriceUseCase findCheapestActiveBasePriceUseCase =
        new FakeFindCheapestActiveBasePriceUseCase();
    private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
    private final UpdatePricingRuleService service = new UpdatePricingRuleService(
        pricingRuleRepository, getProductUseCase, findCheapestActiveBasePriceUseCase, eventPublisher);

    @Test
    void updatesTheRuleAndPublishesPricingRuleUpdated() {
        PricingRule rule = PricingRule.create(
            PricingRuleId.newId(), PricingRuleType.SEGMENT_BASED, new SegmentCondition(CustomerSegment.VIP),
            new GlobalScope(), new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(10)),
            10, CombinationMode.STACKABLE, Instant.now());
        pricingRuleRepository.save(rule);

        UpdatePricingRuleCommand command = new UpdatePricingRuleCommand(
            rule.id(), null, null, CustomerSegment.STUDENT, null, null,
            DiscountType.PERCENTAGE, BigDecimal.valueOf(25), 20, CombinationMode.EXCLUSIVE, "admin@priceforge.com");

        UpdatePricingRuleResult result = service.execute(command);

        assertThat(result.ruleId()).isEqualTo(rule.id());
        assertThat(pricingRuleRepository.findById(rule.id()).get().priority()).isEqualTo(20);
        assertThat(eventPublisher.publishedEvents()).anyMatch(PricingRuleUpdated.class::isInstance);
    }

    @Test
    void rejectsUpdatingARuleThatDoesNotExist() {
        UpdatePricingRuleCommand command = new UpdatePricingRuleCommand(
            PricingRuleId.newId(), null, null, CustomerSegment.STUDENT, null, null,
            DiscountType.PERCENTAGE, BigDecimal.valueOf(25), 20, CombinationMode.EXCLUSIVE, "admin@priceforge.com");

        assertThatThrownBy(() -> service.execute(command)).isInstanceOf(PricingRuleNotFoundException.class);
    }
}
