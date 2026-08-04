package ru.stockmann.replenishment.services.weeklydata.process;

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

    public WeeklyDataValidator() {
        this(new DWHValueParser());
    }

    public WeeklyDataValidator(DWHValueParser parser) {
        this.parser = parser;
    }

    public List<WeeklyDataValidationError> validate(WeeklyDataRawRow row) {
        return validateAndMap(row).errors();
    }

    public WeeklyDataRowValidationResult validateAndMap(WeeklyDataRawRow row) {
        List<WeeklyDataValidationError> errors = new ArrayList<>();

        DWHParseResult<Short> year21 = parseSmallint(errors, row, "Year21", row.year21(), false, false);
        DWHParseResult<Short> week21 = parseSmallint(errors, row, "Week21", row.week21(), false, true);
        DWHParseResult<Short> yearCorr = parseSmallint(errors, row, "YearCorr", row.yearCorr(), false, false);
        DWHParseResult<Short> weekCorr = parseSmallint(errors, row, "WeekCorr", row.weekCorr(), false, true);
        DWHParseResult<Short> year = parseSmallint(errors, row, "Year", row.year(), true, false);
        DWHParseResult<Short> week = parseSmallint(errors, row, "Week", row.week(), true, true);

        DWHParseResult<BigDecimal> totalStockPcs =
                parseDecimal(errors, row, "TotalStockPcs", row.totalStockPcs());
        DWHParseResult<BigDecimal> totalStockDdp =
                parseDecimal(errors, row, "TotalStockDdp", row.totalStockDdp());
        DWHParseResult<BigDecimal> salesPcs = parseDecimal(errors, row, "SalesPcs", row.salesPcs());
        DWHParseResult<BigDecimal> salesRub = parseDecimal(errors, row, "SalesRub", row.salesRub());
        DWHParseResult<BigDecimal> revenue = parseDecimal(errors, row, "Revenue", row.revenue());
        DWHParseResult<BigDecimal> gp = parseDecimal(errors, row, "Gp", row.gp());
        DWHParseResult<BigDecimal> discountTotalRub =
                parseDecimal(errors, row, "DiscountTotalRub", row.discountTotalRub());

        String salesChannelBpo = cleanText(errors, row, "SalesChannelBpo", row.salesChannelBpo());
        String storeRusBpo = cleanText(errors, row, "StoreRusBpo", row.storeRusBpo());
        String storeRus = cleanText(errors, row, "StoreRus", row.storeRus());
        String mfpDivisionNew = cleanText(errors, row, "MfpDivisionNew", row.mfpDivisionNew());
        String mfpDepartment = cleanText(errors, row, "MfpDepartment", row.mfpDepartment());
        String skuSeasonBudget = cleanText(errors, row, "SkuSeasonBudget", row.skuSeasonBudget());
        String typeOfSales = cleanText(errors, row, "TypeOfSales", row.typeOfSales());
        String mfpDivision = cleanText(errors, row, "MfpDivision", row.mfpDivision());
        String season = cleanText(errors, row, "Season", row.season());
        String month = cleanText(errors, row, "Month", row.month());
        String bundle = cleanText(errors, row, "Bundle", row.bundle());
        String seasonality = cleanText(errors, row, "Seasonality", row.seasonality());

        if (!errors.isEmpty()) {
            return new WeeklyDataRowValidationResult(null, errors);
        }

        WeeklyDataStageRow stageRow = new WeeklyDataStageRow(
                row.loadSessionId(),
                row.excelRowNum(),
                year21.value(),
                week21.value(),
                yearCorr.value(),
                weekCorr.value(),
                year.value(),
                week.value(),
                salesChannelBpo,
                storeRusBpo,
                storeRus,
                mfpDivisionNew,
                mfpDepartment,
                skuSeasonBudget,
                typeOfSales,
                valueOrZero(totalStockPcs),
                valueOrZero(totalStockDdp),
                valueOrZero(salesPcs),
                valueOrZero(salesRub),
                valueOrZero(revenue),
                valueOrZero(gp),
                valueOrZero(discountTotalRub),
                mfpDivision,
                season,
                month,
                bundle,
                seasonality,
                row.rawId()
        );
        return new WeeklyDataRowValidationResult(stageRow, List.of());
    }

    private DWHParseResult<Short> parseSmallint(
            List<WeeklyDataValidationError> errors,
            WeeklyDataRawRow row,
            String fieldName,
            String value,
            boolean required,
            boolean week
    ) {
        if (required && !isRequiredPresent(value)) {
            errors.add(error(row, fieldName, "REQUIRED_FIELD_EMPTY", "Required value is empty"));
            return DWHParseResult.success(null, value, null, "SMALLINT");
        }

        DWHParseResult<Short> result = parser.parseSmallint(value);
        if (!result.success()) {
            errors.add(error(row, fieldName, result.errorCode(), "Invalid SMALLINT value"));
        } else if (week && result.value() != null
                && (result.value() < WEEK_MIN || result.value() > WEEK_MAX)) {
            errors.add(error(row, fieldName, "VALUE_OUT_OF_RANGE", "Week value must be between 1 and 100"));
        }
        return result;
    }

    private DWHParseResult<BigDecimal> parseDecimal(
            List<WeeklyDataValidationError> errors,
            WeeklyDataRawRow row,
            String fieldName,
            String value
    ) {
        DWHParseResult<BigDecimal> result = parser.parseDecimal(value);
        if (!result.success()) {
            errors.add(error(row, fieldName, result.errorCode(), "Invalid numeric format"));
        }
        return result;
    }

    private String cleanText(
            List<WeeklyDataValidationError> errors,
            WeeklyDataRawRow row,
            String fieldName,
            String value
    ) {
        String cleaned = parser.cleanText(value);
        if (cleaned != null && cleaned.length() > TEXT_MAX_LENGTH) {
            errors.add(error(row, fieldName, "TEXT_TOO_LONG", "Value exceeds max length 255"));
        }
        return cleaned;
    }

    private boolean isRequiredPresent(String value) {
        String cleaned = parser.cleanText(value);
        return cleaned != null && !parser.isSpecialNull(cleaned);
    }

    private BigDecimal valueOrZero(DWHParseResult<BigDecimal> result) {
        return result.value() != null ? result.value() : BigDecimal.ZERO;
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
