package ru.stockmann.replenishment.services.dwhexcelload.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DWHValueParserTest {

    private final DWHValueParser parser = new DWHValueParser();

    @Test
    void cleanTextReturnsNullForEmptyValues() {
        assertNull(parser.cleanText(null));
        assertNull(parser.cleanText(""));
        assertNull(parser.cleanText("   "));
        assertNull(parser.cleanText("\u00A0\u202F"));
    }

    @Test
    void cleanTextTrimsSupportedSpaces() {
        assertEquals("abc", parser.cleanText("\u00A0abc\u00A0"));
        assertEquals("abc", parser.cleanText("\u202Fabc\u202F"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-", "--", "–", "—", "N/A", "n/a", "NA", "na", "#N/A", "NULL", "null"})
    void allParsersReturnNullForSpecialNullValues(String value) {
        assertSuccessfulNull(parser.parseSmallint(value));
        assertSuccessfulNull(parser.parseInteger(value));
        assertSuccessfulNull(parser.parseLong(value));
        assertSuccessfulNull(parser.parseDecimal(value, 18, 2));
        assertSuccessfulNull(parser.parseDate(value));
    }

    @Test
    void integralParsersAcceptMathematicallyIntegralValues() {
        assertSuccessfulSmallint("12.0", (short) 12);
        assertSuccessfulInteger("1.2e1", 12);
        assertSuccessfulLong("9 223", 9223L);
        assertSuccessfulInteger("+ 1 2", 12);
        assertSuccessfulInteger("- 1 2", -12);
    }

    @Test
    void integralParsersRejectFractionalValuesWithoutRounding() {
        assertError(parser.parseSmallint("12.5"), "INVALID_SMALLINT");
        assertError(parser.parseInteger("1.25e1"), "INVALID_INTEGER");
        assertError(parser.parseLong("12,0001"), "INVALID_BIGINT");
    }

    @Test
    void integralParsersCheckTypeRanges() {
        assertSuccessfulSmallint("-32768", Short.MIN_VALUE);
        assertSuccessfulSmallint("32767", Short.MAX_VALUE);
        assertError(parser.parseSmallint("32768"), "NUMERIC_OVERFLOW");

        assertSuccessfulInteger("-2147483648", Integer.MIN_VALUE);
        assertSuccessfulInteger("2147483647", Integer.MAX_VALUE);
        assertError(parser.parseInteger("2147483648"), "NUMERIC_OVERFLOW");

        assertSuccessfulLong("-9223372036854775808", Long.MIN_VALUE);
        assertSuccessfulLong("9223372036854775807", Long.MAX_VALUE);
        assertError(parser.parseLong("9223372036854775808"), "NUMERIC_OVERFLOW");
    }

    @Test
    void numericParsersAcceptCommaPointSpacesAndScientificNotation() {
        assertSuccessfulDecimal("1 234,56", 18, 2, "1234.56");
        assertSuccessfulDecimal("1\u00A0234.56", 18, 2, "1234.56");
        assertSuccessfulDecimal("1\u202F234,56", 18, 2, "1234.56");
        assertSuccessfulDecimal(".5", 18, 2, "0.50");
        assertSuccessfulDecimal("12.", 18, 2, "12.00");
        assertSuccessfulDecimal("1.25e1", 18, 2, "12.50");
    }

    @Test
    void numericParsersRejectMixedOrRepeatedSeparators() {
        assertError(parser.parseDecimal("1.234,56", 18, 2), "INVALID_DECIMAL");
        assertError(parser.parseDecimal("1,234.56", 18, 2), "INVALID_DECIMAL");
        assertError(parser.parseDecimal("1.2.3", 18, 2), "INVALID_DECIMAL");
        assertError(parser.parseDecimal("1,2,3", 18, 2), "INVALID_DECIMAL");
    }

    @ParameterizedTest
    @ValueSource(strings = {"NaN", "Infinity", "+Infinity", "-Infinity"})
    void numericParsersRejectNonFiniteValues(String value) {
        assertError(parser.parseInteger(value), "INVALID_INTEGER");
        assertError(parser.parseDecimal(value, 18, 2), "INVALID_DECIMAL");
    }

    @Test
    void numericParsersRejectUnsupportedThousandsSeparators() {
        assertError(parser.parseInteger("1_234"), "INVALID_INTEGER");
        assertError(parser.parseInteger("1'234"), "INVALID_INTEGER");
        assertError(parser.parseInteger("1’234"), "INVALID_INTEGER");
    }

    @Test
    void decimalParserRoundsHalfUpAndChecksPrecisionAfterRounding() {
        assertSuccessfulDecimal("12.344", 18, 2, "12.34");
        assertSuccessfulDecimal("12.345", 18, 2, "12.35");
        assertSuccessfulDecimal("-12.345", 18, 2, "-12.35");
        assertSuccessfulDecimal("9999999999999999.994", 18, 2, "9999999999999999.99");
        assertError(
                parser.parseDecimal("9999999999999999.995", 18, 2),
                "NUMERIC_OVERFLOW"
        );
    }

    @Test
    void decimalParserNormalizesNegativeZero() {
        DWHParseResult<BigDecimal> result = parser.parseDecimal("-0.00", 18, 2);

        assertTrue(result.success());
        assertEquals(new BigDecimal("0.00"), result.value());
        assertEquals("0.00", result.normalizedValue());
    }

    @Test
    void decimalParserValidatesDefinition() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseDecimal("1", 0, 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseDecimal("1", 2, 3)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2025-12-25",
            "2025-1-2",
            "25.12.2025",
            "1.2.2025",
            "25/12/2025",
            "1/2/2025"
    })
    void dateParserAcceptsSupportedDateFormats(String value) {
        DWHParseResult<LocalDate> result = parser.parseDate(value);

        assertTrue(result.success());
        assertTrue(result.value().getYear() >= 2025);
        assertEquals(result.value().toString(), result.normalizedValue());
    }

    @Test
    void slashDateIsAlwaysDayMonthYear() {
        DWHParseResult<LocalDate> result = parser.parseDate("03/04/2025");

        assertTrue(result.success());
        assertEquals(LocalDate.of(2025, 4, 3), result.value());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2025-12-25 14:30",
            "2025-12-25 14:30:00",
            "2025-12-25 14:30:00.123",
            "2025-12-25T14:30",
            "2025-12-25T14:30:00",
            "2025-12-25T14:30:00.123"
    })
    void dateParserAcceptsSupportedDateTimeFormatsAndDropsTime(String value) {
        DWHParseResult<LocalDate> result = parser.parseDate(value);

        assertTrue(result.success());
        assertEquals(LocalDate.of(2025, 12, 25), result.value());
        assertEquals("2025-12-25", result.normalizedValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "31.02.2025",
            "29.02.2025",
            "2025-13-01",
            "2025-12-25 24:00:00",
            "2025-12-25 12:60:00",
            "2025-12-25 12:30:60"
    })
    void dateParserUsesStrictCalendarValidation(String value) {
        assertError(parser.parseDate(value), "INVALID_DATE");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "12/25/2025",
            "12/25/25",
            "25.12.25",
            "20250101",
            "25 Dec 2025",
            "2025/12/25",
            "25-12-2025",
            "14:30",
            "2025-12-25T14:30:00Z"
    })
    void dateParserRejectsUnsupportedFormats(String value) {
        assertError(parser.parseDate(value), "INVALID_DATE");
    }

    @Test
    void dateParserConvertsExcelSerialDates() {
        assertSuccessfulDate("1", LocalDate.of(1900, 1, 1));
        assertSuccessfulDate("59", LocalDate.of(1900, 2, 28));
        assertSuccessfulDate("61", LocalDate.of(1900, 3, 1));
        assertSuccessfulDate("61.75", LocalDate.of(1900, 3, 1));
        assertSuccessfulDate("6 1,75", LocalDate.of(1900, 3, 1));
    }

    @Test
    void dateParserRejectsExcelFakeLeapDay() {
        assertError(parser.parseDate("60"), "INVALID_DATE");
        assertError(parser.parseDate("60.5"), "INVALID_DATE");
    }

    @Test
    void dateParserChecksExcelSerialRange() {
        assertError(parser.parseDate("0"), "DATE_OUT_OF_RANGE");
        assertError(parser.parseDate("60001"), "DATE_OUT_OF_RANGE");
        assertError(parser.parseDate("100000"), "DATE_OUT_OF_RANGE");
        assertError(parser.parseDate("999999"), "DATE_OUT_OF_RANGE");
    }

    @Test
    void resultContainsOriginalNormalizedAndTargetType() {
        DWHParseResult<Integer> result = parser.parseInteger(" 1 2,0 ");

        assertTrue(result.success());
        assertEquals(" 1 2,0 ", result.originalValue());
        assertEquals("12", result.normalizedValue());
        assertEquals("INTEGER", result.targetType());
    }

    private void assertSuccessfulSmallint(String value, short expected) {
        DWHParseResult<Short> result = parser.parseSmallint(value);
        assertTrue(result.success());
        assertEquals(expected, result.value());
    }

    private void assertSuccessfulInteger(String value, int expected) {
        DWHParseResult<Integer> result = parser.parseInteger(value);
        assertTrue(result.success());
        assertEquals(expected, result.value());
    }

    private void assertSuccessfulLong(String value, long expected) {
        DWHParseResult<Long> result = parser.parseLong(value);
        assertTrue(result.success());
        assertEquals(expected, result.value());
    }

    private void assertSuccessfulDecimal(
            String value,
            int precision,
            int scale,
            String expected
    ) {
        DWHParseResult<BigDecimal> result = parser.parseDecimal(value, precision, scale);

        assertTrue(result.success());
        assertEquals(new BigDecimal(expected), result.value());
    }

    private void assertSuccessfulDate(String value, LocalDate expected) {
        DWHParseResult<LocalDate> result = parser.parseDate(value);

        assertTrue(result.success());
        assertEquals(expected, result.value());
    }

    private void assertSuccessfulNull(DWHParseResult<?> result) {
        assertTrue(result.success());
        assertNull(result.value());
    }

    private void assertError(DWHParseResult<?> result, String expectedCode) {
        assertFalse(result.success());
        assertNull(result.value());
        assertEquals(expectedCode, result.errorCode());
    }
}
