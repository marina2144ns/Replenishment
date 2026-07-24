package ru.stockmann.replenishment.services.weeklydata.process;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHParseResult;
import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHValueParser;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyDataValidatorTest {

    private final WeeklyDataValidator validator = new WeeklyDataValidator();

    @Test
    void validMinimalRowHasNoErrors() {
        List<WeeklyDataValidationError> errors = validator.validate(row()
                .year("2025")
                .week("10")
                .build());

        assertTrue(errors.isEmpty());
    }

    @Test
    void emptyYearReturnsRequiredFieldError() {
        List<WeeklyDataValidationError> errors = validator.validate(row()
                .year(null)
                .week("10")
                .build());

        assertHasError(errors, "Year", "REQUIRED_FIELD_EMPTY");
    }

    @Test
    void blankAndWhitespaceYearReturnRequiredFieldError() {
        assertHasError(validator.validate(row().year("").week("10").build()), "Year", "REQUIRED_FIELD_EMPTY");
        assertHasError(validator.validate(row().year("   ").week("10").build()), "Year", "REQUIRED_FIELD_EMPTY");
    }

    @Test
    void invalidYearReturnsSmallintError() {
        assertHasError(validator.validate(row().year("abc").week("10").build()), "Year", "INVALID_SMALLINT");
    }

    @Test
    void yearSmallintOverflowReturnsNumericOverflow() {
        assertHasError(validator.validate(row().year("32768").week("10").build()), "Year", "NUMERIC_OVERFLOW");
    }

    @Test
    void emptyWeekReturnsRequiredFieldError() {
        List<WeeklyDataValidationError> errors = validator.validate(row()
                .year("2025")
                .week(null)
                .build());

        assertHasError(errors, "Week", "REQUIRED_FIELD_EMPTY");
    }

    @Test
    void blankAndWhitespaceWeekReturnRequiredFieldError() {
        assertHasError(validator.validate(row().year("2025").week("").build()), "Week", "REQUIRED_FIELD_EMPTY");
        assertHasError(validator.validate(row().year("2025").week("   ").build()), "Week", "REQUIRED_FIELD_EMPTY");
    }

    @Test
    void invalidWeekReturnsSmallintError() {
        assertHasError(validator.validate(row().year("2025").week("abc").build()), "Week", "INVALID_SMALLINT");
    }

    @Test
    void weekSmallintOverflowReturnsNumericOverflow() {
        assertHasError(validator.validate(row().year("2025").week("32768").build()), "Week", "NUMERIC_OVERFLOW");
    }

    @Test
    void weekBelowRangeReturnsRangeError() {
        List<WeeklyDataValidationError> errors = validator.validate(row()
                .year("2025")
                .week("0")
                .build());

        assertHasError(errors, "Week", "VALUE_OUT_OF_RANGE");
    }

    @Test
    void weekAboveRangeReturnsRangeError() {
        List<WeeklyDataValidationError> errors = validator.validate(row()
                .year("2025")
                .week("101")
                .build());

        assertHasError(errors, "Week", "VALUE_OUT_OF_RANGE");
    }

    @Test
    void week21AboveRangeReturnsRangeError() {
        List<WeeklyDataValidationError> errors = validator.validate(row()
                .year("2025")
                .week("10")
                .week21("101")
                .build());

        assertHasError(errors, "Week21", "VALUE_OUT_OF_RANGE");
    }

    @Test
    void invalidDecimalReturnsDecimalError() {
        List<WeeklyDataValidationError> errors = validator.validate(row()
                .year("2025")
                .week("10")
                .salesRub("abc")
                .build());

        assertHasError(errors, "SalesRub", "INVALID_DECIMAL");
    }

    @Test
    void textLongerThan255ReturnsTextTooLongError() {
        List<WeeklyDataValidationError> errors = validator.validate(row()
                .year("2025")
                .week("10")
                .storeRus("a".repeat(256))
                .build());

        assertHasError(errors, "StoreRus", "TEXT_TOO_LONG");
    }

    @Test
    void textLength255IsValidForAllTextFields() {
        for (String fieldName : textFields()) {
            List<WeeklyDataValidationError> errors = validator.validate(rowWithTextField(fieldName, "a".repeat(255)));

            assertTrue(
                    errors.stream().noneMatch(error -> fieldName.equals(error.fieldName())),
                    "Expected no text length error for " + fieldName + ", actual errors: " + errors
            );
        }
    }

    @Test
    void textLength256ReturnsTextTooLongForAllTextFieldsAndKeepsExcelRowNum() {
        for (String fieldName : textFields()) {
            List<WeeklyDataValidationError> errors = validator.validate(rowWithTextField(fieldName, "a".repeat(256)));

            WeeklyDataValidationError error = errors.stream()
                    .filter(e -> fieldName.equals(e.fieldName()))
                    .findFirst()
                    .orElseThrow();
            assertEquals("TEXT_TOO_LONG", error.errorCode(), fieldName);
            assertEquals(3L, error.excelRowNum(), fieldName);
        }
    }

    @Test
    void multipleErrorsInOneRowAreAllReturned() {
        List<WeeklyDataValidationError> errors = validator.validate(row()
                .year("")
                .week("101")
                .salesRub("abc")
                .storeRus("a".repeat(256))
                .build());

        assertEquals(4, errors.size());
        assertHasError(errors, "Year", "REQUIRED_FIELD_EMPTY");
        assertHasError(errors, "Week", "VALUE_OUT_OF_RANGE");
        assertHasError(errors, "SalesRub", "INVALID_DECIMAL");
        assertHasError(errors, "StoreRus", "TEXT_TOO_LONG");
    }

    @Test
    void validRowIsParsedOnceAndMappedDirectlyToStageWithExcelRowNumber() {
        CountingParser parser = new CountingParser();
        WeeklyDataValidator typedValidator = new WeeklyDataValidator(parser);

        WeeklyDataRowValidationResult result = typedValidator.validateAndMap(row()
                .year("2025")
                .week("10")
                .salesRub("12.50")
                .build());

        assertTrue(result.valid());
        assertEquals(6, parser.smallintCalls);
        assertEquals(7, parser.decimalCalls);
        assertEquals(3L, result.stageRow().excelRowNum());
        assertEquals(new BigDecimal("12.50"), result.stageRow().salesRub());
    }

    private void assertHasError(List<WeeklyDataValidationError> errors, String fieldName, String errorCode) {
        assertTrue(
                errors.stream().anyMatch(error ->
                        fieldName.equals(error.fieldName()) && errorCode.equals(error.errorCode())),
                "Expected error " + errorCode + " for field " + fieldName + ", actual errors: " + errors
        );
    }

    private RowBuilder row() {
        return new RowBuilder();
    }

    private static List<String> textFields() {
        return List.of(
                "SalesChannelBpo",
                "StoreRusBpo",
                "StoreRus",
                "MfpDivisionNew",
                "MfpDepartment",
                "SkuSeasonBudget",
                "TypeOfSales",
                "MfpDivision",
                "Season",
                "Month",
                "Bundle",
                "Seasonality"
        );
    }

    private static WeeklyDataRawRow rowWithTextField(String fieldName, String value) {
        return new WeeklyDataRawRow(
                1,
                2,
                3L,
                null,
                null,
                null,
                null,
                "2025",
                "10",
                "SalesChannelBpo".equals(fieldName) ? value : null,
                "StoreRusBpo".equals(fieldName) ? value : null,
                "StoreRus".equals(fieldName) ? value : null,
                "MfpDivisionNew".equals(fieldName) ? value : null,
                "MfpDepartment".equals(fieldName) ? value : null,
                "SkuSeasonBudget".equals(fieldName) ? value : null,
                "TypeOfSales".equals(fieldName) ? value : null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "MfpDivision".equals(fieldName) ? value : null,
                "Season".equals(fieldName) ? value : null,
                "Month".equals(fieldName) ? value : null,
                "Bundle".equals(fieldName) ? value : null,
                "Seasonality".equals(fieldName) ? value : null
        );
    }

    private static class RowBuilder {
        private String year;
        private String week;
        private String week21;
        private String storeRus;
        private String salesRub;

        RowBuilder year(String year) {
            this.year = year;
            return this;
        }

        RowBuilder week(String week) {
            this.week = week;
            return this;
        }

        RowBuilder week21(String week21) {
            this.week21 = week21;
            return this;
        }

        RowBuilder storeRus(String storeRus) {
            this.storeRus = storeRus;
            return this;
        }

        RowBuilder salesRub(String salesRub) {
            this.salesRub = salesRub;
            return this;
        }

        WeeklyDataRawRow build() {
            return new WeeklyDataRawRow(
                    1,
                    2,
                    3L,
                    null,
                    week21,
                    null,
                    null,
                    year,
                    week,
                    null,
                    null,
                    storeRus,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    salesRub,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    private static final class CountingParser extends DWHValueParser {
        private int smallintCalls;
        private int decimalCalls;

        @Override
        public DWHParseResult<Short> parseSmallint(String value) {
            smallintCalls++;
            return super.parseSmallint(value);
        }

        @Override
        public DWHParseResult<BigDecimal> parseDecimal(String value) {
            decimalCalls++;
            return super.parseDecimal(value);
        }
    }
}
