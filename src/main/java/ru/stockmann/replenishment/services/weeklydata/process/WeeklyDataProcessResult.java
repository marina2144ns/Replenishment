package ru.stockmann.replenishment.services.weeklydata.process;

public record WeeklyDataProcessResult(
        long loadSessionId,
        boolean success,
        long totalRows,
        long stagedRows,
        long loadedRows,
        long errorRows,
        String message
) {
}
