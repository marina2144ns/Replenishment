package ru.stockmann.replenishment.services.dwhexcelload.core;

public record DWHExcelLoadSessionResult(
        Long loadSessionId,
        boolean success,
        String message
) {
    public static DWHExcelLoadSessionResult ok(Long loadSessionId) {
        return new DWHExcelLoadSessionResult(loadSessionId, true, null);
    }

    public static DWHExcelLoadSessionResult error(Long loadSessionId, String message) {
        return new DWHExcelLoadSessionResult(loadSessionId, false, message);
    }
}