package ru.stockmann.replenishment.services.dwhexcelload.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DWHValueParserTest {

    private final DWHValueParser parser = new DWHValueParser();

    @Test
    void cleanTextReturnsNullForEmptyValues() {
        assertNull(parser.cleanText(null));
        assertNull(parser.cleanText(""));
        assertNull(parser.cleanText("   "));
    }

    @Test
    void cleanTextTrimsNbspAndNarrowNbsp() {
        assertEquals("abc", parser.cleanText("\u00A0abc\u00A0"));
        assertEquals("abc", parser.cleanText("\u202Fabc\u202F"));
    }

    @Test
    void parseSmallintAcceptsSpacesInsideNumber() {
        assertSuccessfulSmallint("2025", (short) 2025);
        assertSuccessfulSmallint("2 025", (short) 2025);
        assertSuccessfulSmallint("2\u00A0025", (short) 2025);
        assertSuccessfulSmallint("2\u202F025", (short) 2025);
    }

    @Test
    void parseSmallintReturnsErrorForInvalidValue() {
        DWHParseResult<Short> result = parser.parseSmallint("abc");

        assertFalse(result.success());
        assertNull(result.value());
        assertEquals("INVALID_SMALLINT", result.errorCode());
    }

    @Test
    void parseDecimalAcceptsCommaAndSpacesInsideNumber() {
        assertSuccessfulDecimal("123,45", "123.45");
        assertSuccessfulDecimal("1 234,56", "1234.56");
        assertSuccessfulDecimal("1\u00A0234,56", "1234.56");
        assertSuccessfulDecimal("1\u202F234,56", "1234.56");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-", "--", "–", "—", "N/A", "NA", "#N/A", "NULL"})
    void parseDecimalReturnsNullForSpecialNullValues(String value) {
        DWHParseResult<BigDecimal> result = parser.parseDecimal(value);

        assertTrue(result.success());
        assertNull(result.value());
    }

    @Test
    void parseDecimalReturnsErrorForInvalidValue() {
        DWHParseResult<BigDecimal> result = parser.parseDecimal("abc");

        assertFalse(result.success());
        assertNull(result.value());
        assertEquals("INVALID_DECIMAL", result.errorCode());
    }

    private void assertSuccessfulSmallint(String value, short expected) {
        DWHParseResult<Short> result = parser.parseSmallint(value);

        assertTrue(result.success());
        assertEquals(expected, result.value());
    }

    private void assertSuccessfulDecimal(String value, String expected) {
        DWHParseResult<BigDecimal> result = parser.parseDecimal(value);

        assertTrue(result.success());
        assertEquals(new BigDecimal(expected), result.value());
    }
}
