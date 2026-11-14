package com.priceforge.audit.application.port.in;

import com.priceforge.audit.application.dto.AuditEntryDetails;
import com.priceforge.audit.application.dto.EntityChangeHistoryQuery;
import java.util.List;

public interface QueryEntityChangeHistoryUseCase {

    List<AuditEntryDetails> execute(EntityChangeHistoryQuery query);
}
