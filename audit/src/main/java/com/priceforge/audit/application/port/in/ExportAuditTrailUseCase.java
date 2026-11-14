package com.priceforge.audit.application.port.in;

import com.priceforge.audit.application.dto.ExportAuditTrailQuery;
import com.priceforge.audit.application.dto.ExportedAuditTrail;

public interface ExportAuditTrailUseCase {

    ExportedAuditTrail execute(ExportAuditTrailQuery query);
}
