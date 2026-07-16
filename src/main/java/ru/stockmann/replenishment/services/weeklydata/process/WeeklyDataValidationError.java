package ru.stockmann.replenishment.services.weeklydata.process;

public record WeeklyDataValidationError(
        long loadSessionId,
        long rawId,
        Long excelRowNum,
        String errorLayer,
        String fieldName,
        String errorCode,
        String errorReason,
        String errorMessage
) {
}
