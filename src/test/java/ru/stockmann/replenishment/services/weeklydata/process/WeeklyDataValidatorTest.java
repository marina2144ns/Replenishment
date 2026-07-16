package ru.stockmann.replenishment.services.weeklydata.process;

import org.junit.jupiter.api.Test;

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
    void emptyWeekReturnsRequiredFieldError() {
        List<WeeklyDataValidationError> errors = validator.validate(row()
                .year("2025")
                .week("")
                .build());

        assertHasError(errors, "Week", "REQUIRED_FIELD_EMPTY");
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
}
