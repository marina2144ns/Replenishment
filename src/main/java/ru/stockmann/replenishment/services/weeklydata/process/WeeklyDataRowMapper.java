package ru.stockmann.replenishment.services.weeklydata.process;

import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHParseResult;
import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHValueParser;

import java.math.BigDecimal;

public class WeeklyDataRowMapper {

    private final DWHValueParser parser;

    public WeeklyDataRowMapper() {
        this(new DWHValueParser());
    }

    public WeeklyDataRowMapper(DWHValueParser parser) {
        this.parser = parser;
    }

    public WeeklyDataTargetRow toTargetRow(WeeklyDataRawRow row) {
        Short year = parseSmallint(row.year(), "Year");
        Short week = parseSmallint(row.week(), "Week");

        if (year == null) {
            throw new IllegalStateException("Year is required");
        }

        if (week == null) {
            throw new IllegalStateException("Week is required");
        }

        return new WeeklyDataTargetRow(
                row.loadSessionId(),
                parseSmallint(row.year21(), "Year21"),
                parseSmallint(row.week21(), "Week21"),
                parseSmallint(row.yearCorr(), "YearCorr"),
                parseSmallint(row.weekCorr(), "WeekCorr"),
                year,
                week,
                parser.cleanText(row.salesChannelBpo()),
                parser.cleanText(row.storeRusBpo()),
                parser.cleanText(row.storeRus()),
                parser.cleanText(row.mfpDivisionNew()),
                parser.cleanText(row.mfpDepartment()),
                parser.cleanText(row.skuSeasonBudget()),
                parser.cleanText(row.typeOfSales()),
                parseDecimalOrZero(row.totalStockPcs(), "TotalStockPcs"),
                parseDecimalOrZero(row.totalStockDdp(), "TotalStockDdp"),
                parseDecimalOrZero(row.salesPcs(), "SalesPcs"),
                parseDecimalOrZero(row.salesRub(), "SalesRub"),
                parseDecimalOrZero(row.revenue(), "Revenue"),
                parseDecimalOrZero(row.gp(), "Gp"),
                parseDecimalOrZero(row.discountTotalRub(), "DiscountTotalRub"),
                parser.cleanText(row.mfpDivision()),
                parser.cleanText(row.season()),
                parser.cleanText(row.month()),
                parser.cleanText(row.bundle()),
                parser.cleanText(row.seasonality())
        );
    }

    private Short parseSmallint(String value, String fieldName) {
        DWHParseResult<Short> result = parser.parseSmallint(value);
        if (!result.success()) {
            throw new IllegalStateException("Invalid SMALLINT value in field " + fieldName);
        }
        return result.value();
    }

    private BigDecimal parseDecimalOrZero(String value, String fieldName) {
        DWHParseResult<BigDecimal> result = parser.parseDecimal(value);
        if (!result.success()) {
            throw new IllegalStateException("Invalid DECIMAL value in field " + fieldName);
        }
        return result.value() != null ? result.value() : BigDecimal.ZERO;
    }
}
