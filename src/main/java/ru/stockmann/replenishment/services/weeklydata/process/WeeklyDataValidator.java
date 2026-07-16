package ru.stockmann.replenishment.services.weeklydata.process;

import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHFieldValidator;
import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHParseResult;
import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHValueParser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class WeeklyDataValidator {

    private static final String ERROR_LAYER = "VALIDATION";
    private static final int TEXT_MAX_LENGTH = 255;
    private static final int WEEK_MIN = 1;
    private static final int WEEK_MAX = 100;

    private final DWHValueParser parser;
    private final DWHFieldValidator fieldValidator;

    public WeeklyDataValidator() {
        this(new DWHValueParser());
    }

    public WeeklyDataValidator(DWHValueParser parser) {
        this.parser = parser;
        this.fieldValidator = new DWHFieldValidator(parser);
    }

    public List<WeeklyDataValidationError> validate(WeeklyDataRawRow row) {
        List<WeeklyDataValidationError> errors = new ArrayList<>();

        validateRequiredSmallint(errors, row, "Year", row.year());
        validateRequiredWeek(errors, row, "Week", row.week());

        validateOptionalSmallint(errors, row, "Year21", row.year21());
        validateOptionalWeek(errors, row, "Week21", row.week21());
        validateOptionalSmallint(errors, row, "YearCorr", row.yearCorr());
        validateOptionalWeek(errors, row, "WeekCorr", row.weekCorr());

        validateDecimal(errors, row, "TotalStockPcs", row.totalStockPcs());
        validateDecimal(errors, row, "TotalStockDdp", row.totalStockDdp());
        validateDecimal(errors, row, "SalesPcs", row.salesPcs());
        validateDecimal(errors, row, "SalesRub", row.salesRub());
        validateDecimal(errors, row, "Revenue", row.revenue());
        validateDecimal(errors, row, "Gp", row.gp());
        validateDecimal(errors, row, "DiscountTotalRub", row.discountTotalRub());

        validateText(errors, row, "SalesChannelBpo", row.salesChannelBpo());
        validateText(errors, row, "StoreRusBpo", row.storeRusBpo());
        validateText(errors, row, "StoreRus", row.storeRus());
        validateText(errors, row, "MfpDivisionNew", row.mfpDivisionNew());
        validateText(errors, row, "MfpDepartment", row.mfpDepartment());
        validateText(errors, row, "SkuSeasonBudget", row.skuSeasonBudget());
        validateText(errors, row, "TypeOfSales", row.typeOfSales());
        validateText(errors, row, "MfpDivision", row.mfpDivision());
        validateText(errors, row, "Season", row.season());
        validateText(errors, row, "Month", row.month());
        validateText(errors, row, "Bundle", row.bundle());
        validateText(errors, row, "Seasonality", row.seasonality());

        return errors;
    }

    private void validateRequiredSmallint(
            List<WeeklyDataValidationError> errors,
            WeeklyDataRawRow row,
            String fieldName,
            String value
    ) {
        if (!fieldValidator.isRequiredPresent(value)) {
            errors.add(error(row, fieldName, "REQUIRED_FIELD_EMPTY", "Required value is empty"));
            return;
        }

        validateOptionalSmallint(errors, row, fieldName, value);
    }

    private void validateRequiredWeek(
            List<WeeklyDataValidationError> errors,
            WeeklyDataRawRow row,
            String fieldName,
            String value
    ) {
        if (!fieldValidator.isRequiredPresent(value)) {
            errors.add(error(row, fieldName, "REQUIRED_FIELD_EMPTY", "Required value is empty"));
            return;
        }

        validateOptionalWeek(errors, row, fieldName, value);
    }

    private void validateOptionalSmallint(
            List<WeeklyDataValidationError> errors,
            WeeklyDataRawRow row,
            String fieldName,
            String value
    ) {
        DWHParseResult<Short> result = parser.parseSmallint(value);
        if (!result.success()) {
            errors.add(error(row, fieldName, result.errorCode(), "Invalid SMALLINT value"));
        }
    }

    private void validateOptionalWeek(
            List<WeeklyDataValidationError> errors,
            WeeklyDataRawRow row,
            String fieldName,
            String value
    ) {
        DWHParseResult<Short> result = parser.parseSmallint(value);
        if (!result.success()) {
            errors.add(error(row, fieldName, result.errorCode(), "Invalid SMALLINT value"));
            return;
        }

        if (result.value() != null && !fieldValidator.isInRange(result.value(), WEEK_MIN, WEEK_MAX)) {
            errors.add(error(row, fieldName, "VALUE_OUT_OF_RANGE", "Week value must be between 1 and 100"));
        }
    }

    private void validateDecimal(
            List<WeeklyDataValidationError> errors,
            WeeklyDataRawRow row,
            String fieldName,
            String value
    ) {
        DWHParseResult<BigDecimal> result = parser.parseDecimal(value);
        if (!result.success()) {
            errors.add(error(row, fieldName, result.errorCode(), "Invalid numeric format"));
        }
    }

    private void validateText(
            List<WeeklyDataValidationError> errors,
            WeeklyDataRawRow row,
            String fieldName,
            String value
    ) {
        if (!fieldValidator.isTextLengthValid(value, TEXT_MAX_LENGTH)) {
            errors.add(error(row, fieldName, "TEXT_TOO_LONG", "Value exceeds max length 255"));
        }
    }

    private WeeklyDataValidationError error(
            WeeklyDataRawRow row,
            String fieldName,
            String errorCode,
            String errorReason
    ) {
        return new WeeklyDataValidationError(
                row.loadSessionId(),
                row.rawId(),
                row.excelRowNum(),
                ERROR_LAYER,
                fieldName,
                errorCode,
                errorReason,
                "RawId=" + row.rawId() + ". " + errorReason + " in field [" + fieldName + "]."
        );
    }
}
