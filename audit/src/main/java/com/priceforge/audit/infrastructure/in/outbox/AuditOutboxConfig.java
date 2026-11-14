package com.priceforge.audit.infrastructure.in.outbox;

import com.priceforge.audit.application.port.in.RecordAuditEntryUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One {@link AuditOutboxEventHandler} bean per audited event type: the relay groups handlers by
 * {@code eventType()}, so each type needs its own instance, and they must be individual beans for
 * the relay's {@code List<OutboxEventHandler<?>>} injection to collect them. Keep in sync with
 * {@link AuditEntryTranslator#AUDITED_EVENT_TYPES} (a test enforces it).
 */
@Configuration
public class AuditOutboxConfig {

    @Bean
    AuditOutboxEventHandler auditProductCreatedHandler(AuditEntryTranslator translator, RecordAuditEntryUseCase useCase) {
        return new AuditOutboxEventHandler("ProductCreated", translator, useCase);
    }

    @Bean
    AuditOutboxEventHandler auditProductPriceBaseUpdatedHandler(AuditEntryTranslator translator, RecordAuditEntryUseCase useCase) {
        return new AuditOutboxEventHandler("ProductPriceBaseUpdated", translator, useCase);
    }

    @Bean
    AuditOutboxEventHandler auditProductDeactivatedHandler(AuditEntryTranslator translator, RecordAuditEntryUseCase useCase) {
        return new AuditOutboxEventHandler("ProductDeactivated", translator, useCase);
    }

    @Bean
    AuditOutboxEventHandler auditProductCategoryChangedHandler(AuditEntryTranslator translator, RecordAuditEntryUseCase useCase) {
        return new AuditOutboxEventHandler("ProductCategoryChanged", translator, useCase);
    }

    @Bean
    AuditOutboxEventHandler auditPricingRuleCreatedHandler(AuditEntryTranslator translator, RecordAuditEntryUseCase useCase) {
        return new AuditOutboxEventHandler("PricingRuleCreated", translator, useCase);
    }

    @Bean
    AuditOutboxEventHandler auditPricingRuleUpdatedHandler(AuditEntryTranslator translator, RecordAuditEntryUseCase useCase) {
        return new AuditOutboxEventHandler("PricingRuleUpdated", translator, useCase);
    }

    @Bean
    AuditOutboxEventHandler auditPricingRuleDeactivatedHandler(AuditEntryTranslator translator, RecordAuditEntryUseCase useCase) {
        return new AuditOutboxEventHandler("PricingRuleDeactivated", translator, useCase);
    }

    @Bean
    AuditOutboxEventHandler auditPriceCalculatedHandler(AuditEntryTranslator translator, RecordAuditEntryUseCase useCase) {
        return new AuditOutboxEventHandler("PriceCalculated", translator, useCase);
    }

    @Bean
    AuditOutboxEventHandler auditRuleConflictDetectedHandler(AuditEntryTranslator translator, RecordAuditEntryUseCase useCase) {
        return new AuditOutboxEventHandler("RuleConflictDetected", translator, useCase);
    }

    @Bean
    AuditOutboxEventHandler auditPromotionCreatedHandler(AuditEntryTranslator translator, RecordAuditEntryUseCase useCase) {
        return new AuditOutboxEventHandler("PromotionCreated", translator, useCase);
    }

    @Bean
    AuditOutboxEventHandler auditPromotionAppliedHandler(AuditEntryTranslator translator, RecordAuditEntryUseCase useCase) {
        return new AuditOutboxEventHandler("PromotionApplied", translator, useCase);
    }

    @Bean
    AuditOutboxEventHandler auditPromotionActivatedHandler(AuditEntryTranslator translator, RecordAuditEntryUseCase useCase) {
        return new AuditOutboxEventHandler("PromotionActivated", translator, useCase);
    }

    @Bean
    AuditOutboxEventHandler auditPromotionExpiredHandler(AuditEntryTranslator translator, RecordAuditEntryUseCase useCase) {
        return new AuditOutboxEventHandler("PromotionExpired", translator, useCase);
    }

    @Bean
    AuditOutboxEventHandler auditPromotionExhaustedHandler(AuditEntryTranslator translator, RecordAuditEntryUseCase useCase) {
        return new AuditOutboxEventHandler("PromotionExhausted", translator, useCase);
    }
}
