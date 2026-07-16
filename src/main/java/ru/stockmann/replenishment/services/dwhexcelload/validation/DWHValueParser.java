package ru.stockmann.replenishment.services.dwhexcelload.validation;

import java.math.BigDecimal;

public class DWHValueParser {

    public String cleanText(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value
                .replace('\u00A0', ' ')
                .replace('\u202F', ' ')
                .trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    public DWHParseResult<Short> parseSmallint(String value) {
        String cleaned = cleanText(value);

        if (isEmpty(cleaned)) {
            return new DWHParseResult<>(null, true, null, null);
        }

        String normalized = removeNumberSpaces(cleaned);

        try {
            return new DWHParseResult<>(Short.parseShort(normalized), true, null, null);
        } catch (NumberFormatException e) {
            return new DWHParseResult<>(
                    null,
                    false,
                    "INVALID_SMALLINT",
                    "Invalid SMALLINT value: " + value
            );
        }
    }

    public DWHParseResult<BigDecimal> parseDecimal(String value) {
        String cleaned = cleanText(value);

        if (isEmpty(cleaned) || isSpecialNull(cleaned)) {
            return new DWHParseResult<>(null, true, null, null);
        }

        String normalized = cleaned
                .replace(" ", "")
                .replace("\u00A0", "")
                .replace("\u202F", "")
                .replace(",", ".");

        try {
            return new DWHParseResult<>(new BigDecimal(normalized), true, null, null);
        } catch (NumberFormatException e) {
            return new DWHParseResult<>(
                    null,
                    false,
                    "INVALID_DECIMAL",
                    "Invalid DECIMAL value: " + value
            );
        }
    }

    public boolean isEmpty(String value) {
        return value == null || value.isBlank();
    }

    public boolean isSpecialNull(String value) {
        if (value == null) {
            return false;
        }

        String cleaned = cleanText(value);
        return "-".equals(cleaned)
                || "--".equals(cleaned)
                || "–".equals(cleaned)
                || "—".equals(cleaned)
                || "N/A".equalsIgnoreCase(cleaned)
                || "NA".equalsIgnoreCase(cleaned)
                || "#N/A".equalsIgnoreCase(cleaned)
                || "NULL".equalsIgnoreCase(cleaned);
    }

    private String removeNumberSpaces(String value) {
        return value
                .replace(" ", "")
                .replace("\u00A0", "")
                .replace("\u202F", "");
    }
}
