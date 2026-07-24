package ru.stockmann.replenishment.services.cddata.process;

public record CDDataProcessResult(
        long loadSessionId,
        boolean success,
        long totalRows,
        long stagedRows,
        long loadedRows,
        long errorRows,
        String message
) {
    public CDDataProcessResult(
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
