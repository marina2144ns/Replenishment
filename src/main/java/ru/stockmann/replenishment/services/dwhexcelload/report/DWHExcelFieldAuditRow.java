package ru.stockmann.replenishment.services.dwhexcelload.report;

public record DWHExcelFieldAuditRow(
        String loadCode,
        int excelIndex,
        String excelColumnName,
        String rawColumnName,
        String targetColumnName,
        String valueKind,
        Integer rawMaxLength,
        boolean required,
        String javaValidation,
        String sqlValidation
) {
}