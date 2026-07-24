package ru.stockmann.replenishment.services.salesbychannel.process;

public record SalesByChannelValidationError(
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
