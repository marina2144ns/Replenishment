package ru.stockmann.replenishment.services.dwhexcelload.definitions;

import ru.stockmann.replenishment.services.dwhexcelload.core.*;

import java.util.List;

public class WeeklyDataExcelLoadDefinition implements DWHExcelLoadDefinition {

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
        return "dbo.usp_Weekly_data_ProcessLoadSession";
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
                DWHExcelColumns.intNumber(0, "Year21", 50, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.intNumber(1, "Week21", 50, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.intNumber(2, "YearCorr", 50, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.intNumber(3, "WeekCorr", 50, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.intNumber(4, "Year", 50, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.intNumber(5, "Week", 50, DWHExcelNullHandling.KEEP_NULL),

                DWHExcelColumns.text(6, "SalesChannelBpo", 255),
                DWHExcelColumns.text(7, "StoreRusBpo", 255),
                DWHExcelColumns.text(8, "StoreRus", 255),
                DWHExcelColumns.text(9, "MfpDivisionNew", 255),
                DWHExcelColumns.text(10, "MfpDepartment", 255),
                DWHExcelColumns.text(11, "SkuSeasonBudget", 100),
                DWHExcelColumns.text(12, "TypeOfSales", 100),

                DWHExcelColumns.decimal(13, "TotalStockPcs", 50, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(14, "TotalStockDdp", 50, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(15, "SalesPcs", 50, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(16, "SalesRub", 50, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(17, "Revenue", 50, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(18, "Gp", 50, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(19, "DiscountTotalRub", 50, DWHExcelNullHandling.ZERO),

                DWHExcelColumns.text(20, "MfpDivision", 255),
                DWHExcelColumns.text(21, "Season", 100),
                DWHExcelColumns.text(22, "Month", 100),
                DWHExcelColumns.text(23, "Bundle", 100),
                DWHExcelColumns.text(24, "Seasonality", 100)
        );
    }
}