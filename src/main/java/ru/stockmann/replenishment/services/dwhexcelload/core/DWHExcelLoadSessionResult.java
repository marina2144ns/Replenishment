package ru.stockmann.replenishment.services.dwhexcelload.core;

public record DWHExcelLoadSessionResult(
        Long loadSessionId,
        boolean success,
        Long totalRows,
        Long loadedRows,
        Long errorRows,
        String message
) {
    public DWHExcelLoadSessionResult(Long loadSessionId, boolean success, String message) {
        this(loadSessionId, success, null, null, null, message);
    }

    public static DWHExcelLoadSessionResult ok(Long loadSessionId) {
        return new DWHExcelLoadSessionResult(loadSessionId, true, null, null, null, null);
    }

    public static DWHExcelLoadSessionResult error(Long loadSessionId, String message) {
        return new DWHExcelLoadSessionResult(loadSessionId, false, null, null, null, message);
    }
}
