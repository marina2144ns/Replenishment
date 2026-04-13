package ru.stockmann.replenishment.services.dwhexcelload.core;

public record DWHExcelLoadResult(
        Long loadSessionId,
        String status,
        String message
) {
    public static DWHExcelLoadResult ok(Long loadSessionId, String message) {
        return new DWHExcelLoadResult(loadSessionId, "OK", message);
    }

    public static DWHExcelLoadResult error(Long loadSessionId, String message) {
        return new DWHExcelLoadResult(loadSessionId, "ERROR", message);
    }
}