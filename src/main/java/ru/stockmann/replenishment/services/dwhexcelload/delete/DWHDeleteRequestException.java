package ru.stockmann.replenishment.services.dwhexcelload.delete;

public class DWHDeleteRequestException extends IllegalArgumentException {

    private final String code;

    public DWHDeleteRequestException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static DWHDeleteRequestException invalidDataset(String dataset) {
        return new DWHDeleteRequestException(
                "INVALID_DATASET",
                "Dataset '" + String.valueOf(dataset) + "' is not available"
        );
    }

    public static DWHDeleteRequestException emptyFilter() {
        return new DWHDeleteRequestException(
                "EMPTY_DELETE_FILTER",
                "At least one delete filter is required"
        );
    }

    public static DWHDeleteRequestException invalidField(String field, String dataset) {
        return new DWHDeleteRequestException(
                "INVALID_DELETE_FIELD",
                "Field '" + field + "' is not available for dataset " + dataset
        );
    }

    public static DWHDeleteRequestException invalidValue(
            String field,
            String expectedType,
            String detail
    ) {
        return new DWHDeleteRequestException(
                "INVALID_FILTER_VALUE",
                "Invalid value for field '" + field + "' (expected " + expectedType + "): " + detail
        );
    }
}
