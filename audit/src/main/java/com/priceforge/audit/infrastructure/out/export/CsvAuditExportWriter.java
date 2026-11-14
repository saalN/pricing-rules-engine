package com.priceforge.audit.infrastructure.out.export;

import com.priceforge.audit.application.dto.AuditExportFormat;
import com.priceforge.audit.application.port.out.AuditExportWriterPort;
import com.priceforge.audit.domain.model.AuditEntry;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * RFC 4180 CSV. Cells that start with a formula trigger ({@code = + - @}) are prefixed with a
 * single quote: the export is meant to be opened in a spreadsheet, and the stored states and
 * names ultimately come from user input.
 */
@Component
public class CsvAuditExportWriter implements AuditExportWriterPort {

    private static final String HEADER =
        "eventId,eventType,entityType,entityId,productId,userId,changedBy,occurredAt,"
            + "previousState,newState,reason,payload";

    @Override
    public AuditExportFormat format() {
        return AuditExportFormat.CSV;
    }

    @Override
    public String contentType() {
        return "text/csv";
    }

    @Override
    public String fileExtension() {
        return "csv";
    }

    @Override
    public String write(List<AuditEntry> entries) {
        return Stream.concat(Stream.of(HEADER), entries.stream().map(CsvAuditExportWriter::row))
            .collect(Collectors.joining("\r\n", "", "\r\n"));
    }

    private static String row(AuditEntry entry) {
        return Stream.of(
                entry.eventId().toString(), entry.eventType(), entry.entityType().name(), entry.entityId(),
                entry.productId(), entry.userId(), entry.changedBy(), entry.occurredAt().toString(),
                entry.previousState(), entry.newState(), entry.reason(), entry.payload())
            .map(CsvAuditExportWriter::cell)
            .collect(Collectors.joining(","));
    }

    private static String cell(String value) {
        if (value == null) {
            return "";
        }
        String safe = startsWithFormulaTrigger(value) ? "'" + value : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    private static boolean startsWithFormulaTrigger(String value) {
        char first = value.charAt(0);
        return first == '=' || first == '+' || first == '-' || first == '@';
    }
}
