package ru.stockmann.replenishment.services.dwhexcelload.core;

import ru.stockmann.replenishment.services.dwhexcelload.normalizers.DWHExcelValueNormalizer;

public record DWHExcelColumnSpec(
        int excelIndex,
        String excelColumnName,
        String rawColumnName,
        String targetColumnName,
        DWHExcelValueKind valueKind,
        int rawMaxLength,
        boolean required,
        DWHExcelValueNormalizer normalizer,
        DWHExcelNullHandling nullHandling,
        String javaValidationNote,
        String sqlValidationNote
) {
}