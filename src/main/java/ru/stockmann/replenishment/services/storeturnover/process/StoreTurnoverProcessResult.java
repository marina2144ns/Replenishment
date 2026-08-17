package ru.stockmann.replenishment.services.storeturnover.process;

public record StoreTurnoverProcessResult(
        long loadSessionId, boolean success, long totalRows, long stagedRows,
        long loadedRows, long errorRows, String message
) {}
