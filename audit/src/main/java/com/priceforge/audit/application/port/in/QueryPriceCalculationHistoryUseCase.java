package com.priceforge.audit.application.port.in;

import com.priceforge.audit.application.dto.AuditEntryPageResult;
import com.priceforge.audit.application.dto.PriceCalculationHistoryQuery;

public interface QueryPriceCalculationHistoryUseCase {

    AuditEntryPageResult execute(PriceCalculationHistoryQuery query);
}
