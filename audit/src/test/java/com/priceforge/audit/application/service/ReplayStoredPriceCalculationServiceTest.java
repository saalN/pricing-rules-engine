package com.priceforge.audit.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.audit.application.dto.ReplayStoredPriceCalculationCommand;
import com.priceforge.audit.application.dto.ReplayStoredPriceCalculationResult;
import com.priceforge.audit.application.service.support.AuditEntries;
import com.priceforge.audit.application.service.support.FakePriceReplayPort;
import com.priceforge.audit.application.service.support.InMemoryAuditEntryRepositoryPort;
import com.priceforge.audit.domain.exception.PriceCalculationNotFoundException;
import com.priceforge.audit.domain.model.AuditEntityType;
import com.priceforge.audit.domain.model.AuditEntry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReplayStoredPriceCalculationServiceTest {

    private static final Instant T1 = Instant.parse("2026-07-20T10:00:00Z");

    private final InMemoryAuditEntryRepositoryPort repository = new InMemoryAuditEntryRepositoryPort();
    private final FakePriceReplayPort priceReplay = new FakePriceReplayPort();
    private final ReplayStoredPriceCalculationService service =
        new ReplayStoredPriceCalculationService(repository, priceReplay);

    @Test
    void reportsAMatchWhenTheCurrentRulesGiveTheSamePrice() {
        AuditEntry stored = AuditEntries.priceCalculation(
            "p-1", "u-1", T1, BigDecimal.valueOf(100), new BigDecimal("85.0000"));
        repository.saveIfAbsent(stored);
        priceReplay.replayTo(new BigDecimal("85.00"));

        ReplayStoredPriceCalculationResult result =
            service.execute(new ReplayStoredPriceCalculationCommand(stored.eventId()));

        assertThat(result.matches()).isTrue();
        assertThat(result.originalFinalPriceAmount()).isEqualByComparingTo("85");
        assertThat(result.replayedFinalPriceAmount()).isEqualByComparingTo("85");
        assertThat(result.currency()).isEqualTo("EUR");
        assertThat(result.originalCalculatedAt()).isEqualTo(T1);
    }

    @Test
    void reportsADifferenceWhenTheRulesHaveChangedSince() {
        AuditEntry stored = AuditEntries.priceCalculation(
            "p-1", "u-1", T1, BigDecimal.valueOf(100), BigDecimal.valueOf(85));
        repository.saveIfAbsent(stored);
        priceReplay.replayTo(BigDecimal.valueOf(100));

        ReplayStoredPriceCalculationResult result =
            service.execute(new ReplayStoredPriceCalculationCommand(stored.eventId()));

        assertThat(result.matches()).isFalse();
        assertThat(result.replayedFinalPriceAmount()).isEqualByComparingTo("100");
    }

    @Test
    void replaysTheStoredContextExactly() {
        AuditEntry stored = AuditEntries.priceCalculation(
            "p-1", "u-1", T1, BigDecimal.valueOf(100), BigDecimal.valueOf(85));
        repository.saveIfAbsent(stored);

        service.execute(new ReplayStoredPriceCalculationCommand(stored.eventId()));

        assertThat(priceReplay.lastSnapshot()).isEqualTo(stored.priceSnapshot());
    }

    @Test
    void rejectsAnUnknownEvent() {
        assertThatThrownBy(() -> service.execute(new ReplayStoredPriceCalculationCommand(UUID.randomUUID())))
            .isInstanceOf(PriceCalculationNotFoundException.class);
    }

    @Test
    void rejectsAnEntryThatIsNotAPriceCalculation() {
        AuditEntry ruleChange = AuditEntries.change(
            AuditEntityType.PRICING_RULE, "rule-1", "PricingRuleCreated", T1, null, "x", null);
        repository.saveIfAbsent(ruleChange);

        assertThatThrownBy(() -> service.execute(new ReplayStoredPriceCalculationCommand(ruleChange.eventId())))
            .isInstanceOf(PriceCalculationNotFoundException.class);
    }

    @Test
    void reportsBothPricesInTheCurrencysDecimals() {
        AuditEntry stored = AuditEntries.priceCalculation(
            "p-1", "u-1", T1, BigDecimal.valueOf(100), new BigDecimal("85.0000"));
        repository.saveIfAbsent(stored);
        priceReplay.replayTo(new BigDecimal("85.000000000000"));

        ReplayStoredPriceCalculationResult result =
            service.execute(new ReplayStoredPriceCalculationCommand(stored.eventId()));

        assertThat(result.originalFinalPriceAmount()).isEqualTo(new BigDecimal("85.00"));
        assertThat(result.replayedFinalPriceAmount()).isEqualTo(new BigDecimal("85.00"));
    }
}
