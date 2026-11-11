package ru.stockmann.replenishment.services.dwhexcelload.core;

/** Common validation and normalization for the optional session initiator. */
public final class DWHRequestedBy {

    public static final int MAX_LENGTH = 100;

    private DWHRequestedBy() {
    }

    public static String normalizeAndValidate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("requestedBy must not be longer than 100 characters");
        }
        return value;
    }

    /** Query value takes precedence whenever the parameter is present, including an empty value. */
    public static String resolve(String queryValue, String jsonValue) {
        return normalizeAndValidate(queryValue != null ? queryValue : jsonValue);
    }
}
