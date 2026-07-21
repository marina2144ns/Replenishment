package ru.stockmann.replenishment.services.cdecom.process;

public record CDEcomValidationError(
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
