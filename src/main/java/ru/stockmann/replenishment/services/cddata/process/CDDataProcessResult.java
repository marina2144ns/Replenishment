package ru.stockmann.replenishment.services.cddata.process;

public record CDDataProcessResult(
        long loadSessionId,
        boolean success,
        long totalRows,
        long loadedRows,
        long errorRows,
        String message
) {
}
