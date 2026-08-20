package ru.stockmann.replenishment.services.dwhexcelload.definitions;

import ru.stockmann.replenishment.services.dwhexcelload.core.*;

import java.util.List;

public class WeeklyDataExcelLoadDefinition implements DWHExcelLoadDefinition {

    private static final int RAW_TEXT_LENGTH = 4000;

    @Override
    public DWHExcelLoadType loadType() {
        return DWHExcelLoadType.WEEKLY_DATA;
    }

    @Override
    public String rawTableName() {
        return "dbo.Weekly_data_raw";
    }

    @Override
    public String targetTableName() {
        return "dbo.Weekly_data";
    }

    @Override
    public String processProcedureName() {
        throw new UnsupportedOperationException("WeeklyData processing is implemented in Java");
    }

    @Override
    public int expectedColumnCount() {
        return 25;
    }

    @Override
    public int batchSize() {
        return 10_000;
    }

    @Override
    public List<DWHExcelColumnSpec> columns() {
        return List.of(
                DWHExcelColumns.intNumber(
                        0, "Year 21", "Year21", "Year21", 50, false, DWHExcelNullHandling.KEEP_NULL
                ),
                DWHExcelColumns.intNumber(
                        1, "Week 21", "Week21", "Week21", 50, false, DWHExcelNullHandling.KEEP_NULL
                ),
                DWHExcelColumns.intNumber(
                        2, "Year _corr", "YearCorr", "YearCorr", 50, false, DWHExcelNullHandling.KEEP_NULL
                ),
                DWHExcelColumns.intNumber(
                        3, "Week_corr", "WeekCorr", "WeekCorr", 50, false, DWHExcelNullHandling.KEEP_NULL
                ),
                DWHExcelColumns.intNumber(
                        4, "Year", "Year", "Year", 50, true, DWHExcelNullHandling.KEEP_NULL
                ),
                DWHExcelColumns.intNumber(
                        5, "Week", "Week", "Week", 50, true, DWHExcelNullHandling.KEEP_NULL
                ),

                DWHExcelColumns.text(6, "Sales Channel_BPO", "SalesChannelBpo", "SalesChannelBpo",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(7, "StoreRus_BPO", "StoreRusBpo", "StoreRusBpo",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(8, "StoreRUS", "StoreRus", "StoreRus",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(9, "MFP Division_new", "MfpDivisionNew", "MfpDivisionNew",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(10, "MFP Department", "MfpDepartment", "MfpDepartment",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(11, "SKU SeasonBudget", "SkuSeasonBudget", "SkuSeasonBudget",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(12, "TypeOfSales", "TypeOfSales", "TypeOfSales",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),

                DWHExcelColumns.decimal(13, "Total Stock, Pcs", "TotalStockPcs", "TotalStockPcs",
                        255, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(14, "Total Stock, DDP", "TotalStockDdp", "TotalStockDdp",
                        255, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(15, "Sales, Pcs", "SalesPcs", "SalesPcs",
                        255, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(16, "Sales, rub", "SalesRub", "SalesRub",
                        255, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(17, "Revenue, rur", "Revenue", "Revenue",
                        255, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(18, "GP", "Gp", "Gp",
                        255, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(19, "Discount Total, rub", "DiscountTotalRub", "DiscountTotalRub",
                        255, false, DWHExcelNullHandling.ZERO),

                DWHExcelColumns.text(20, "MFP Division", "MfpDivision", "MfpDivision",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(21, "Сезон", "Season", "Season",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(22, "Месяц", "Month", "Month",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(23, "Сцепка", "Bundle", "Bundle",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(24, "Seasonality", "Seasonality", "Seasonality",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL)
        );
    }
}
