package ru.stockmann.replenishment.services.dwhexcelload.validation;

public record DWHParseResult<T>(
        T value,
        boolean success,
        String errorCode,
        String errorMessage,
        String originalValue,
        String normalizedValue,
        String targetType
) {

    /**
     * Backward-compatible constructor for existing code.
     */
    public DWHParseResult(
            T value,
            boolean success,
            String errorCode,
            String errorMessage
    ) {
        this(value, success, errorCode, errorMessage, null, null, null);
    }

    public static <T> DWHParseResult<T> success(
            T value,
            String originalValue,
            String normalizedValue,
            String targetType
    ) {
        return new DWHParseResult<>(
                value,
                true,
                null,
                null,
                originalValue,
                normalizedValue,
                targetType
        );
    }

    public static <T> DWHParseResult<T> failure(
            String errorCode,
            String errorMessage,
            String originalValue,
            String normalizedValue,
            String targetType
    ) {
        return new DWHParseResult<>(
                null,
                false,
                errorCode,
                errorMessage,
                originalValue,
                normalizedValue,
                targetType
        );
    }
}
