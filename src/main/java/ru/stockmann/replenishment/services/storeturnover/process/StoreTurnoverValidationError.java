package ru.stockmann.replenishment.services.storeturnover.process;

public record StoreTurnoverValidationError(
        Long loadSessionId, Long rawId, Long excelRowNum, String errorLayer,
        String fieldName, String errorCode, String errorReason, String errorMessage
) {}
