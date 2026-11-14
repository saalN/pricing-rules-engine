package com.priceforge.audit.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.priceforge.audit.application.dto.RecordAuditEntryCommand;
import com.priceforge.audit.application.service.support.AuditEntries;
import com.priceforge.audit.application.service.support.InMemoryAuditEntryRepositoryPort;
import com.priceforge.audit.domain.model.AuditEntityType;
import com.priceforge.audit.domain.model.AuditEntry;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RecordAuditEntryServiceTest {

    private final InMemoryAuditEntryRepositoryPort repository = new InMemoryAuditEntryRepositoryPort();
    private final RecordAuditEntryService service = new RecordAuditEntryService(repository);

    @Test
    void storesTheEntry() {
        AuditEntry entry = entry();

        service.execute(new RecordAuditEntryCommand(entry));

        assertThat(repository.findById(entry.eventId())).contains(entry);
    }

    @Test
    void recordingTheSameEventTwiceKeepsASingleEntry() {
        AuditEntry entry = entry();

        service.execute(new RecordAuditEntryCommand(entry));
        service.execute(new RecordAuditEntryCommand(entry));

        assertThat(repository.size()).isEqualTo(1);
    }

    private static AuditEntry entry() {
        return AuditEntries.change(
            AuditEntityType.PRICING_RULE, "rule-1", "PricingRuleDeactivated", Instant.now(),
            "ACTIVE", "INACTIVE", "admin@priceforge.com");
    }
}
