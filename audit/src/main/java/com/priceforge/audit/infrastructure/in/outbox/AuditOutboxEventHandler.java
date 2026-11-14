package com.priceforge.audit.infrastructure.in.outbox;

import com.priceforge.audit.application.dto.RecordAuditEntryCommand;
import com.priceforge.audit.application.port.in.RecordAuditEntryUseCase;
import com.priceforge.audit.infrastructure.in.outbox.event.RawDomainEvent;
import com.priceforge.sharedkernel.OutboxEventHandler;

/**
 * Records one event type from another context's outbox in the audit trail. The generic relay
 * delivers at-least-once; recording is idempotent on the event id, so a redelivery is harmless.
 */
public class AuditOutboxEventHandler implements OutboxEventHandler<RawDomainEvent> {

    private final String eventType;
    private final AuditEntryTranslator translator;
    private final RecordAuditEntryUseCase recordAuditEntryUseCase;

    public AuditOutboxEventHandler(
            String eventType, AuditEntryTranslator translator, RecordAuditEntryUseCase recordAuditEntryUseCase) {
        this.eventType = eventType;
        this.translator = translator;
        this.recordAuditEntryUseCase = recordAuditEntryUseCase;
    }

    @Override
    public String eventType() {
        return eventType;
    }

    @Override
    public Class<RawDomainEvent> eventClass() {
        return RawDomainEvent.class;
    }

    @Override
    public void handle(RawDomainEvent event) {
        recordAuditEntryUseCase.execute(new RecordAuditEntryCommand(translator.translate(eventType, event)));
    }
}
