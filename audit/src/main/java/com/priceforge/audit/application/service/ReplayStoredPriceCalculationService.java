package com.priceforge.audit.application.service;

import com.priceforge.audit.application.dto.ReplayStoredPriceCalculationCommand;
import com.priceforge.audit.application.dto.ReplayStoredPriceCalculationResult;
import com.priceforge.audit.application.port.in.ReplayStoredPriceCalculationUseCase;
import com.priceforge.audit.application.port.out.AuditEntryRepositoryPort;
import com.priceforge.audit.application.port.out.PriceReplayPort;
import com.priceforge.audit.domain.exception.PriceCalculationNotFoundException;
import com.priceforge.audit.domain.model.AuditEntityType;
import com.priceforge.audit.domain.model.AuditEntry;
import com.priceforge.audit.domain.model.PriceCalculationSnapshot;
import com.priceforge.sharedkernel.Money;
import java.math.BigDecimal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-14, step 3: re-runs a stored calculation from its {@code contextSnapshot} against the rules
 * active now and reports whether the result is the same. Read-only on both sides — the replay
 * publishes no event, so it never adds to the audit trail it is checking.
 */
@Service
public class ReplayStoredPriceCalculationService implements ReplayStoredPriceCalculationUseCase {

    private final AuditEntryRepositoryPort auditEntryRepository;
    private final PriceReplayPort priceReplay;

    public ReplayStoredPriceCalculationService(
            AuditEntryRepositoryPort auditEntryRepository, PriceReplayPort priceReplay) {
        this.auditEntryRepository = auditEntryRepository;
        this.priceReplay = priceReplay;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ReplayStoredPriceCalculationResult execute(ReplayStoredPriceCalculationCommand command) {
        AuditEntry entry = auditEntryRepository.findById(command.eventId())
            .filter(found -> found.entityType() == AuditEntityType.PRICE_CALCULATION)
            .filter(found -> found.priceSnapshot() != null)
            .orElseThrow(() -> new PriceCalculationNotFoundException(command.eventId()));

        PriceCalculationSnapshot snapshot = entry.priceSnapshot();
        BigDecimal replayed = priceReplay.replayFinalPrice(snapshot);

        // The stored column keeps four decimals; report both prices in the currency's own.
        return new ReplayStoredPriceCalculationResult(
            entry.eventId(), entry.occurredAt(), snapshot.currency(),
            inCurrencyDecimals(snapshot.finalPriceAmount(), snapshot.currency()),
            inCurrencyDecimals(replayed, snapshot.currency()),
            snapshot.finalPriceAmount().compareTo(replayed) == 0);
    }

    private static BigDecimal inCurrencyDecimals(BigDecimal amount, String currency) {
        return Money.of(amount, currency).roundedToCurrency().amount();
    }
}
