package ru.stockmann.replenishment.services.cdecom.process;

import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHParseResult;
import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHValueParser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;

class CDEcomValueParser {

    private static final String BIGINT = "BIGINT";
    private static final String DATE = "DATE";
    private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            strict("d.M.uuuu"),
            strict("d/M/uuuu"),
            strict("uuuu-M-d"),
            twoDigitYear("M/d/"),
            strict("M/d/uuuu")
    );

    private final DWHValueParser parser = new DWHValueParser();

    String cleanText(String value) {
        return parser.cleanText(value);
    }

    DWHParseResult<Integer> parseInteger(String value) {
        return parser.parseInteger(value);
    }

    DWHParseResult<Long> parseDirectLong(String value) {
        String cleaned = parser.cleanText(value);
        if (cleaned == null || parser.isSpecialNull(cleaned)) {
            return DWHParseResult.success(null, value, null, BIGINT);
        }

        String normalized = cleaned
                .replace(" ", "")
                .replace("\u00A0", "")
                .replace("\u202F", "");

        if (normalized.contains(",") || isForbiddenSpecialNumber(normalized)) {
            return invalidBigint(value, normalized);
        }

        try {
            BigDecimal decimal = new BigDecimal(normalized).stripTrailingZeros();
            if (decimal.scale() > 0) {
                return invalidBigint(value, normalized);
            }

            BigInteger integer = decimal.toBigIntegerExact();
            if (integer.compareTo(LONG_MIN) < 0 || integer.compareTo(LONG_MAX) > 0) {
                return DWHParseResult.failure(
                        "NUMERIC_OVERFLOW",
                        "BIGINT value is out of range: " + value,
                        value,
                        normalized,
                        BIGINT
                );
            }
            return DWHParseResult.success(integer.longValueExact(), value, integer.toString(), BIGINT);
        } catch (ArithmeticException | NumberFormatException e) {
            return invalidBigint(value, normalized);
        }
    }

    DWHParseResult<BigDecimal> parseDecimal(String value) {
        return parser.parseDecimal(value, 18, 2);
    }

    DWHParseResult<LocalDate> parseDate(String value) {
        String cleaned = parser.cleanText(value);
        if (cleaned == null || parser.isSpecialNull(cleaned)) {
            return DWHParseResult.success(null, value, null, DATE);
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate parsed = LocalDate.parse(cleaned, formatter);
                return DWHParseResult.success(parsed, value, parsed.toString(), DATE);
            } catch (DateTimeParseException ignored) {
            }
        }

        return DWHParseResult.failure(
                "INVALID_DATE",
                "Invalid DATE value: " + value,
                value,
                cleaned,
                DATE
        );
    }

    DWHParseResult<Long> parseRoundedLong(String value) {
        String cleaned = parser.cleanText(value);
        if (cleaned == null || parser.isSpecialNull(cleaned)) {
            return DWHParseResult.success(null, value, null, BIGINT);
        }

        String normalized = cleaned
                .replace(" ", "")
                .replace("\u00A0", "")
                .replace("\u202F", "")
                .replace(',', '.');

        if (normalized.indexOf('.') != normalized.lastIndexOf('.')) {
            return invalidBigint(value, normalized);
        }

        if (isForbiddenSpecialNumber(normalized)) {
            return invalidBigint(value, normalized);
        }

        BigDecimal decimal;
        try {
            decimal = new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            return invalidBigint(value, normalized);
        }

        BigInteger rounded = decimal.setScale(0, RoundingMode.HALF_UP).toBigIntegerExact();
        if (rounded.compareTo(LONG_MIN) < 0 || rounded.compareTo(LONG_MAX) > 0) {
            return DWHParseResult.failure(
                    "NUMERIC_OVERFLOW",
                    "BIGINT value is out of range: " + value,
                    value,
                    normalized,
                    BIGINT
            );
        }

        return DWHParseResult.success(
                rounded.longValueExact(),
                value,
                rounded.toString(),
                BIGINT
        );
    }

    private static DWHParseResult<Long> invalidBigint(String value, String normalized) {
        return DWHParseResult.failure(
                "INVALID_BIGINT",
                "Invalid BIGINT value: " + value,
                value,
                normalized,
                BIGINT
        );
    }

    private static boolean isForbiddenSpecialNumber(String value) {
        return "NAN".equalsIgnoreCase(value)
                || "INFINITY".equalsIgnoreCase(value)
                || "+INFINITY".equalsIgnoreCase(value)
                || "-INFINITY".equalsIgnoreCase(value);
    }

    private static DateTimeFormatter strict(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseSensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ROOT)
                .withResolverStyle(ResolverStyle.STRICT);
    }

    private static DateTimeFormatter twoDigitYear(String prefixPattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseSensitive()
                .appendPattern(prefixPattern)
                .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
                .toFormatter(Locale.ROOT)
                .withResolverStyle(ResolverStyle.STRICT);
    }
}
