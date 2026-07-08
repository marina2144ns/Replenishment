package ru.stockmann.replenishment.services.dwhexcelload.validation;

public record DWHParseResult<T>(
        T value,
        boolean success,
        String errorCode,
        String errorMessage
) {
}
