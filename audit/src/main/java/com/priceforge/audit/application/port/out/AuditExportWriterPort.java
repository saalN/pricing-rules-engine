package com.priceforge.audit.application.port.out;

import com.priceforge.audit.application.dto.AuditExportFormat;
import com.priceforge.audit.domain.model.AuditEntry;
import java.util.List;

/** One implementation per {@link AuditExportFormat}. */
public interface AuditExportWriterPort {

    AuditExportFormat format();

    String contentType();

    String fileExtension();

    String write(List<AuditEntry> entries);
}
