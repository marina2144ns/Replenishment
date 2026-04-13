package ru.stockmann.replenishment.services.dwhexcelload.core;

public enum DWHExcelLoadType {
    CD_DATA("CD_DATA", "CD data"),
    CD_ECOM("CD_ECOM", "CD ecom"),
    WEEKLY_DATA("WEEKLY_DATA", "Weekly data");

    private final String code;
    private final String displayName;

    DWHExcelLoadType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public static DWHExcelLoadType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Load type code is empty");
        }

        for (DWHExcelLoadType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unsupported load type code: " + code);
    }
}