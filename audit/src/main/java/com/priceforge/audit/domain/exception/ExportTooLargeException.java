package com.priceforge.audit.domain.exception;

public class ExportTooLargeException extends RuntimeException {

    public ExportTooLargeException(long matching, int limit) {
        super("The export matches " + matching + " entries, above the limit of " + limit
            + "; narrow it with more filters (date range, product, entity)");
    }
}
