package ru.stockmann.replenishment.services.dwhexcelload.core;

import java.util.Map;

public record ExcelRowData(
        int rowNum,
        Map<String, String> values
) {
    public String get(String rawColumnName) {
        return values.get(rawColumnName);
    }
}