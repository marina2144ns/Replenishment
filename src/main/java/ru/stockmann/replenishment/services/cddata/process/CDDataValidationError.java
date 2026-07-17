package ru.stockmann.replenishment.services.cddata.process;

public record CDDataValidationError(
        Long loadSessionId,
        Long rawId,
        Long excelRowNum,
        String errorLayer,
        String fieldName,
        String errorCode,
        String errorReason,
        String errorMessage
) {
}
