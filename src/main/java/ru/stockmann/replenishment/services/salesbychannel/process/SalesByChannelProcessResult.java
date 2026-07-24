package ru.stockmann.replenishment.services.salesbychannel.process;

public record SalesByChannelProcessResult(
        long loadSessionId,
        boolean success,
        long totalRows,
        long stagedRows,
        long loadedRows,
        long errorRows,
        String message
) {
}
