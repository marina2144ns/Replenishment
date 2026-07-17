package ru.stockmann.replenishment.services.dwhexcelload.validation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Locale;

public class DWHValueParser {

    private static final String SMALLINT = "SMALLINT";
    private static final String INTEGER = "INTEGER";
    private static final String BIGINT = "BIGINT";
    private static final String DECIMAL = "DECIMAL";
    private static final String DATE = "DATE";

    private static final BigInteger SMALLINT_MIN = BigInteger.valueOf(Short.MIN_VALUE);
    private static final BigInteger SMALLINT_MAX = BigInteger.valueOf(Short.MAX_VALUE);
    private static final BigInteger INTEGER_MIN = BigInteger.valueOf(Integer.MIN_VALUE);
    private static final BigInteger INTEGER_MAX = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final BigInteger BIGINT_MIN = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger BIGINT_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    private static final BigDecimal EXCEL_SERIAL_MIN = BigDecimal.ONE;
    private static final BigDecimal EXCEL_SERIAL_UPPER_EXCLUSIVE = new BigDecimal("60001");
    private static final BigInteger EXCEL_FAKE_LEAP_DAY_SERIAL = BigInteger.valueOf(60);
    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 31);

    private static final DateTimeFormatter ISO_DATE =
            strictFormatter("uuuu-M-d");

    private static final DateTimeFormatter DOT_DATE =
            strictFormatter("d.M.uuuu");

    private static final DateTimeFormatter SLASH_DATE =
            strictFormatter("d/M/uuuu");

    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            strictDateTimeFormatter(' ', false),
            strictDateTimeFormatter(' ', true),
            strictDateTimeFormatter('T', false),
            strictDateTimeFormatter('T', true)
    );

    public String cleanText(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = trimSupportedSpaces(value);
        return cleaned.isEmpty() ? null : cleaned;
    }

    public DWHParseResult<Short> parseSmallint(String value) {
        return parseIntegral(
                value,
                SMALLINT,
                "INVALID_SMALLINT",
                SMALLINT_MIN,
                SMALLINT_MAX,
                BigInteger::shortValueExact
        );
    }

    public DWHParseResult<Integer> parseInteger(String value) {
        return parseIntegral(
                value,
                INTEGER,
                "INVALID_INTEGER",
                INTEGER_MIN,
                INTEGER_MAX,
                BigInteger::intValueExact
        );
    }

    public DWHParseResult<Long> parseLong(String value) {
        return parseIntegral(
                value,
                BIGINT,
                "INVALID_BIGINT",
                BIGINT_MIN,
                BIGINT_MAX,
                BigInteger::longValueExact
        );
    }

    /**
     * Backward-compatible method. It parses a decimal without applying
     * precision/scale restrictions.
     */
    public DWHParseResult<BigDecimal> parseDecimal(String value) {
        NumericPreparation preparation = prepareNumeric(value, DECIMAL, "INVALID_DECIMAL");
        if (preparation.result() != null) {
            return cast(preparation.result());
        }

        try {
            BigDecimal parsed = new BigDecimal(preparation.normalized());
            return DWHParseResult.success(
                    normalizeZero(parsed),
                    value,
                    preparation.normalized(),
                    DECIMAL
            );
        } catch (NumberFormatException e) {
            return invalid(
                    "INVALID_DECIMAL",
                    DECIMAL,
                    value,
                    preparation.normalized()
            );
        }
    }

    public DWHParseResult<BigDecimal> parseDecimal(
            String value,
            int precision,
            int scale
    ) {
        validateDecimalDefinition(precision, scale);

        NumericPreparation preparation = prepareNumeric(value, DECIMAL, "INVALID_DECIMAL");
        if (preparation.result() != null) {
            return cast(preparation.result());
        }

        final BigDecimal parsed;
        try {
            parsed = new BigDecimal(preparation.normalized());
        } catch (NumberFormatException e) {
            return invalid(
                    "INVALID_DECIMAL",
                    DECIMAL,
                    value,
                    preparation.normalized()
            );
        }

        BigDecimal rounded = normalizeZero(parsed.setScale(scale, RoundingMode.HALF_UP));

        if (rounded.precision() > precision) {
            return overflow(DECIMAL, value, preparation.normalized());
        }

        return DWHParseResult.success(
                rounded,
                value,
                rounded.toPlainString(),
                DECIMAL
        );
    }

    public DWHParseResult<LocalDate> parseDate(String value) {
        String cleaned = cleanText(value);

        if (cleaned == null || isSpecialNull(cleaned)) {
            return DWHParseResult.success(null, value, null, DATE);
        }

        LocalDate parsedDate = tryParseTextDate(cleaned);
        if (parsedDate != null) {
            if (!isSqlDateRange(parsedDate)) {
                return dateOutOfRange(value, cleaned);
            }
            return DWHParseResult.success(
                    parsedDate,
                    value,
                    parsedDate.toString(),
                    DATE
            );
        }

        DWHParseResult<LocalDate> excelDate = tryParseExcelSerial(value, cleaned);
        if (excelDate != null) {
            return excelDate;
        }

        return invalid("INVALID_DATE", DATE, value, cleaned);
    }

    public boolean isEmpty(String value) {
        return cleanText(value) == null;
    }

    public boolean isSpecialNull(String value) {
        String cleaned = cleanText(value);
        if (cleaned == null) {
            return false;
        }

        String compact = removeNumberSpaces(cleaned);

        return "-".equals(compact)
                || "--".equals(compact)
                || "–".equals(compact)
                || "—".equals(compact)
                || "N/A".equalsIgnoreCase(compact)
                || "NA".equalsIgnoreCase(compact)
                || "#N/A".equalsIgnoreCase(compact)
                || "NULL".equalsIgnoreCase(compact);
    }

    private <T> DWHParseResult<T> parseIntegral(
            String value,
            String targetType,
            String invalidCode,
            BigInteger min,
            BigInteger max,
            BigIntegerConverter<T> converter
    ) {
        NumericPreparation preparation = prepareNumeric(value, targetType, invalidCode);
        if (preparation.result() != null) {
            return cast(preparation.result());
        }

        final BigDecimal decimal;
        try {
            decimal = new BigDecimal(preparation.normalized());
        } catch (NumberFormatException e) {
            return invalid(invalidCode, targetType, value, preparation.normalized());
        }

        final BigInteger integer;
        try {
            integer = decimal.toBigIntegerExact();
        } catch (ArithmeticException e) {
            return invalid(invalidCode, targetType, value, preparation.normalized());
        }

        if (integer.compareTo(min) < 0 || integer.compareTo(max) > 0) {
            return overflow(targetType, value, preparation.normalized());
        }

        try {
            T parsed = converter.convert(integer);
            return DWHParseResult.success(
                    parsed,
                    value,
                    integer.toString(),
                    targetType
            );
        } catch (ArithmeticException e) {
            return overflow(targetType, value, preparation.normalized());
        }
    }

    private NumericPreparation prepareNumeric(
            String originalValue,
            String targetType,
            String invalidCode
    ) {
        String cleaned = cleanText(originalValue);

        if (cleaned == null || isSpecialNull(cleaned)) {
            return new NumericPreparation(
                    null,
                    DWHParseResult.success(null, originalValue, null, targetType)
            );
        }

        String compact = removeNumberSpaces(cleaned);

        if (containsBothDecimalSeparators(compact)) {
            return new NumericPreparation(
                    compact,
                    invalid(invalidCode, targetType, originalValue, compact)
            );
        }

        String normalized = compact.replace(',', '.');

        if (isForbiddenSpecialNumber(normalized)) {
            return new NumericPreparation(
                    normalized,
                    invalid(invalidCode, targetType, originalValue, normalized)
            );
        }

        return new NumericPreparation(normalized, null);
    }

    private LocalDate tryParseTextDate(String value) {
        LocalDate date = tryParseLocalDate(value, ISO_DATE);
        if (date != null) {
            return date;
        }

        date = tryParseLocalDate(value, DOT_DATE);
        if (date != null) {
            return date;
        }

        date = tryParseLocalDate(value, SLASH_DATE);
        if (date != null) {
            return date;
        }

        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter).toLocalDate();
            } catch (DateTimeParseException ignored) {
                // Try the next supported format.
            }
        }

        return null;
    }

    private DWHParseResult<LocalDate> tryParseExcelSerial(
            String originalValue,
            String cleaned
    ) {
        String compact = removeNumberSpaces(cleaned);

        if (containsBothDecimalSeparators(compact)) {
            return invalid("INVALID_DATE", DATE, originalValue, compact);
        }

        String normalized = compact.replace(',', '.');

        if (looksLikeCompactDate(normalized)) {
            return null;
        }

        final BigDecimal serial;
        try {
            serial = new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            return null;
        }

        if (serial.compareTo(EXCEL_SERIAL_MIN) < 0
                || serial.compareTo(EXCEL_SERIAL_UPPER_EXCLUSIVE) >= 0) {
            return dateOutOfRange(originalValue, normalized);
        }

        BigInteger wholeDays = serial.setScale(0, RoundingMode.FLOOR).toBigIntegerExact();

        if (EXCEL_FAKE_LEAP_DAY_SERIAL.equals(wholeDays)) {
            return invalid("INVALID_DATE", DATE, originalValue, normalized);
        }

        BigInteger adjustedDays = wholeDays.compareTo(EXCEL_FAKE_LEAP_DAY_SERIAL) > 0
                ? wholeDays.subtract(BigInteger.ONE)
                : wholeDays;

        try {
            LocalDate date = EXCEL_EPOCH.plusDays(adjustedDays.longValueExact());

            if (!isSqlDateRange(date)) {
                return dateOutOfRange(originalValue, normalized);
            }

            return DWHParseResult.success(
                    date,
                    originalValue,
                    date.toString(),
                    DATE
            );
        } catch (ArithmeticException | DateTimeException e) {
            return dateOutOfRange(originalValue, normalized);
        }
    }

    private static LocalDate tryParseLocalDate(
            String value,
            DateTimeFormatter formatter
    ) {
        try {
            return LocalDate.parse(value, formatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static DateTimeFormatter strictFormatter(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseSensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ROOT)
                .withResolverStyle(ResolverStyle.STRICT);
    }

    private static DateTimeFormatter strictDateTimeFormatter(
            char separator,
            boolean withSeconds
    ) {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
                .parseCaseSensitive()
                .appendPattern("uuuu-M-d")
                .appendLiteral(separator)
                .appendPattern("HH:mm");

        if (withSeconds) {
            builder.appendPattern(":ss")
                    .optionalStart()
                    .appendFraction(ChronoField.NANO_OF_SECOND, 1, 3, true)
                    .optionalEnd();
        }

        return builder
                .toFormatter(Locale.ROOT)
                .withResolverStyle(ResolverStyle.STRICT);
    }

    private static String trimSupportedSpaces(String value) {
        int start = 0;
        int end = value.length();

        while (start < end && isSupportedSpace(value.charAt(start))) {
            start++;
        }
        while (end > start && isSupportedSpace(value.charAt(end - 1))) {
            end--;
        }

        return value.substring(start, end);
    }

    private static boolean isSupportedSpace(char ch) {
        return ch == ' ' || ch == '\u00A0' || ch == '\u202F';
    }

    private static String removeNumberSpaces(String value) {
        return value
                .replace(" ", "")
                .replace("\u00A0", "")
                .replace("\u202F", "");
    }

    private static boolean containsBothDecimalSeparators(String value) {
        return value.indexOf('.') >= 0 && value.indexOf(',') >= 0;
    }

    private static boolean isForbiddenSpecialNumber(String value) {
        return "NAN".equalsIgnoreCase(value)
                || "INFINITY".equalsIgnoreCase(value)
                || "+INFINITY".equalsIgnoreCase(value)
                || "-INFINITY".equalsIgnoreCase(value);
    }

    private static boolean looksLikeCompactDate(String value) {
        if (value.length() != 8) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }

        int month = Integer.parseInt(value.substring(4, 6));
        int day = Integer.parseInt(value.substring(6, 8));

        return month >= 1 && month <= 12 && day >= 1 && day <= 31;
    }

    private static BigDecimal normalizeZero(BigDecimal value) {
        return value.signum() == 0 ? BigDecimal.ZERO.setScale(value.scale()) : value;
    }

    private static void validateDecimalDefinition(int precision, int scale) {
        if (precision <= 0) {
            throw new IllegalArgumentException("precision must be greater than 0");
        }
        if (scale < 0) {
            throw new IllegalArgumentException("scale must be greater than or equal to 0");
        }
        if (scale > precision) {
            throw new IllegalArgumentException("scale must not be greater than precision");
        }
    }

    private static boolean isSqlDateRange(LocalDate date) {
        return date.getYear() >= 1 && date.getYear() <= 9999;
    }

    private static <T> DWHParseResult<T> invalid(
            String errorCode,
            String targetType,
            String originalValue,
            String normalizedValue
    ) {
        return DWHParseResult.failure(
                errorCode,
                "Invalid " + targetType + " value: " + originalValue,
                originalValue,
                normalizedValue,
                targetType
        );
    }

    private static <T> DWHParseResult<T> overflow(
            String targetType,
            String originalValue,
            String normalizedValue
    ) {
        return DWHParseResult.failure(
                "NUMERIC_OVERFLOW",
                targetType + " value is out of range: " + originalValue,
                originalValue,
                normalizedValue,
                targetType
        );
    }

    private static <T> DWHParseResult<T> dateOutOfRange(
            String originalValue,
            String normalizedValue
    ) {
        return DWHParseResult.failure(
                "DATE_OUT_OF_RANGE",
                "DATE value is out of range: " + originalValue,
                originalValue,
                normalizedValue,
                DATE
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> DWHParseResult<T> cast(DWHParseResult<?> result) {
        return (DWHParseResult<T>) result;
    }

    @FunctionalInterface
    private interface BigIntegerConverter<T> {
        T convert(BigInteger value);
    }

    private record NumericPreparation(
            String normalized,
            DWHParseResult<?> result
    ) {
    }
}
