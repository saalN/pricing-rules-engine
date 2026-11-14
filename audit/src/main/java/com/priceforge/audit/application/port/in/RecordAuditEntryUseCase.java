package com.priceforge.audit.application.port.in;

import com.priceforge.audit.application.dto.RecordAuditEntryCommand;

/** Internal use case driven by the outbox event adapters — not exposed over HTTP, so no RBAC. */
public interface RecordAuditEntryUseCase {

    void execute(RecordAuditEntryCommand command);
}
