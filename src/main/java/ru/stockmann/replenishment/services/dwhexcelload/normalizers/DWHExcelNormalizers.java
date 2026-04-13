package ru.stockmann.replenishment.services.dwhexcelload.normalizers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public final class DWHExcelNormalizers {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("d.M.yyyy", Locale.ROOT),
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT),
            DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ROOT),
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT),
            DateTimeFormatter.ISO_LOCAL_DATE
    );

    private DWHExcelNormalizers() {
    }

    public static final DWHExcelValueNormalizer TRIM_TO_NULL = raw -> {
        if (raw == null) {
            return null;
        }
        String v = raw.trim();
        return v.isEmpty() ? null : v;
    };

    public static final DWHExcelValueNormalizer TEXT = TRIM_TO_NULL;

    public static final DWHExcelValueNormalizer NUMERIC_TEXT = raw -> {
        if (raw == null) {
            return null;
        }
        String v = raw
                .replace('\u00A0', ' ')
                .replace('\u202F', ' ')
                .trim();
        return v.isEmpty() ? null : v;
    };

    public static final DWHExcelValueNormalizer DATE_TO_ISO = raw -> {
        if (raw == null) {
            return null;
        }
        String v = raw.trim();
        if (v.isEmpty()) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(v, formatter).toString(); // yyyy-MM-dd
            } catch (DateTimeParseException ignored) {
            }
        }

        return v;
    };
}