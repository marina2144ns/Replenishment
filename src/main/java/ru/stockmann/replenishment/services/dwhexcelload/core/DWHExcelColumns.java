package ru.stockmann.replenishment.services.dwhexcelload.core;

import ru.stockmann.replenishment.services.dwhexcelload.normalizers.DWHExcelNormalizers;

public final class DWHExcelColumns {

    private DWHExcelColumns() {
    }

    public static DWHExcelColumnSpec text(
            int index,
            String name,
            int rawLength
    ) {
        return text(index, name, name, name, rawLength, false, DWHExcelNullHandling.KEEP_NULL);
    }

    public static DWHExcelColumnSpec text(
            int index,
            String excelColumnName,
            String rawColumnName,
            String targetColumnName,
            int rawLength,
            boolean required,
            DWHExcelNullHandling nullHandling
    ) {
        return new DWHExcelColumnSpec(
                index,
                excelColumnName,
                rawColumnName,
                targetColumnName,
                DWHExcelValueKind.TEXT,
                rawLength,
                required,
                DWHExcelNormalizers.TEXT,
                nullHandling,
                "trim + empty->null",
                "NULLIF(LTRIM(RTRIM(...)), '') + LEN<=" + rawLength
        );
    }

    public static DWHExcelColumnSpec intNumber(
            int index,
            String name,
            int rawLength,
            DWHExcelNullHandling nullHandling
    ) {
        return intNumber(index, name, name, name, rawLength, false, nullHandling);
    }

    public static DWHExcelColumnSpec intNumber(
            int index,
            String excelColumnName,
            String rawColumnName,
            String targetColumnName,
            int rawLength,
            boolean required,
            DWHExcelNullHandling nullHandling
    ) {
        return new DWHExcelColumnSpec(
                index,
                excelColumnName,
                rawColumnName,
                targetColumnName,
                DWHExcelValueKind.INT,
                rawLength,
                required,
                DWHExcelNormalizers.NUMERIC_TEXT,
                nullHandling,
                "trim numeric text + remove NBSP/narrow NBSP",
                "TRY_CONVERT(INT)"
        );
    }
    public static DWHExcelColumnSpec decimal(
            int index,
            String name,
            int rawLength,
            DWHExcelNullHandling nullHandling
    ) {
        return decimal(index, name, name, name, rawLength, false, nullHandling);
    }
    public static DWHExcelColumnSpec decimal(
            int index,
            String excelColumnName,
            String rawColumnName,
            String targetColumnName,
            int rawLength,
            boolean required,
            DWHExcelNullHandling nullHandling
    ) {
        return new DWHExcelColumnSpec(
                index,
                excelColumnName,
                rawColumnName,
                targetColumnName,
                DWHExcelValueKind.DECIMAL,
                rawLength,
                required,
                DWHExcelNormalizers.NUMERIC_TEXT,
                nullHandling,
                "trim numeric text + remove NBSP/narrow NBSP",
                "REPLACE(','->'.') + TRY_CONVERT(DECIMAL(18,2))"
        );
    }



    public static DWHExcelColumnSpec decimalFloatValidation(
            int index,
            String name,
            int rawLength,
            DWHExcelNullHandling nullHandling
    ) {
        return decimalFloatValidation(index, name, name, name, rawLength, false, nullHandling);
    }

    public static DWHExcelColumnSpec decimalFloatValidation(
            int index,
            String excelColumnName,
            String rawColumnName,
            String targetColumnName,
            int rawLength,
            boolean required,
            DWHExcelNullHandling nullHandling
    ) {
        return new DWHExcelColumnSpec(
                index,
                excelColumnName,
                rawColumnName,
                targetColumnName,
                DWHExcelValueKind.DECIMAL,
                rawLength,
                required,
                DWHExcelNormalizers.NUMERIC_TEXT,
                nullHandling,
                "trim numeric text + remove NBSP/narrow NBSP",
                "REPLACE(','->'.') + TRY_CONVERT(FLOAT) in validation; target DECIMAL(18,2)"
        );
    }

    public static DWHExcelColumnSpec date(
            int index,
            String name,
            int rawLength
    ) {
        return date(index, name, name, name, rawLength, false, DWHExcelNullHandling.KEEP_NULL);
    }

    public static DWHExcelColumnSpec date(
            int index,
            String excelColumnName,
            String rawColumnName,
            String targetColumnName,
            int rawLength,
            boolean required,
            DWHExcelNullHandling nullHandling
    ) {
        return new DWHExcelColumnSpec(
                index,
                excelColumnName,
                rawColumnName,
                targetColumnName,
                DWHExcelValueKind.DATE,
                rawLength,
                required,
                DWHExcelNormalizers.DATE_TO_ISO,
                nullHandling,
                "trim + normalize to yyyy-MM-dd if possible",
                "TRY_CONVERT(DATE, current proc may use style-specific conversion)"
        );
    }
}