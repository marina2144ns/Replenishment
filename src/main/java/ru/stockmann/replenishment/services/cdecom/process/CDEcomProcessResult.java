package ru.stockmann.replenishment.services.cdecom.process;

public record CDEcomProcessResult(
        long loadSessionId,
        boolean success,
        long totalRows,
        long stagedRows,
        long loadedRows,
        long errorRows,
        String message
) {
    public CDEcomProcessResult(
            long loadSessionId,
            boolean success,
            long totalRows,
            long loadedRows,
            long errorRows,
            String message
    ) {
        this(loadSessionId, success, totalRows, loadedRows, loadedRows, errorRows, message);
    }
}
