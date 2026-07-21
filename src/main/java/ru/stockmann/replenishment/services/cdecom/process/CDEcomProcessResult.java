package ru.stockmann.replenishment.services.cdecom.process;

public record CDEcomProcessResult(
        long loadSessionId,
        boolean success,
        long totalRows,
        long loadedRows,
        long errorRows,
        String message
) {
}
