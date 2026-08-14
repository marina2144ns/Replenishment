package ru.stockmann.replenishment.services.dwhexcelload.definitions;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelValueKind;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelNullHandling;

import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyDataExcelLoadDefinitionTest {

    private static final Set<String> ZERO_METRICS = Set.of(
            "TotalStockPcs", "TotalStockDdp", "SalesPcs", "SalesRub",
            "Revenue", "Gp", "DiscountTotalRub"
    );

    private static final Set<String> TEXT_COLUMNS = Set.of(
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

    @Test
    void declaresExactHeaderContractAndColumnCount() {
        WeeklyDataExcelLoadDefinition definition = new WeeklyDataExcelLoadDefinition();

        assertEquals(25, definition.expectedColumnCount());
        assertEquals(List.of(
                "Year21", "Week21", "YearCorr", "WeekCorr", "Year", "Week",
                "SalesChannelBpo", "StoreRusBpo", "StoreRus", "MfpDivisionNew",
                "MfpDepartment", "SkuSeasonBudget", "TypeOfSales", "TotalStockPcs",
                "TotalStockDdp", "SalesPcs", "SalesRub", "Revenue", "Gp",
                "DiscountTotalRub", "MfpDivision", "Season", "Month", "Bundle",
                "Seasonality"
        ), definition.columns().stream().map(column -> column.excelColumnName()).toList());
    }

    @Test
    void rawTextColumnsAllowValuesLongerThanTargetLength() {
        WeeklyDataExcelLoadDefinition definition = new WeeklyDataExcelLoadDefinition();

        definition.columns().forEach(column -> {
            if (TEXT_COLUMNS.contains(column.rawColumnName())) {
                assertEquals(4000, column.rawMaxLength(), column.rawColumnName());
                assertEquals(DWHExcelValueKind.TEXT, column.valueKind(), column.rawColumnName());
            }
        });
    }

    @Test
    void numericColumnsDeclareRequiredDimensionsAndOptionalZeroMetricsExplicitly() {
        WeeklyDataExcelLoadDefinition definition = new WeeklyDataExcelLoadDefinition();

        definition.columns().stream()
                .filter(column -> ZERO_METRICS.contains(column.rawColumnName()))
                .forEach(column -> {
                    assertEquals(DWHExcelValueKind.DECIMAL, column.valueKind(), column.rawColumnName());
                    assertEquals(false, column.required(), column.rawColumnName());
                    assertEquals(DWHExcelNullHandling.ZERO, column.nullHandling(), column.rawColumnName());
                });

        for (String required : List.of("Year", "Week")) {
            var column = definition.columns().stream()
                    .filter(candidate -> required.equals(candidate.rawColumnName()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(DWHExcelValueKind.INT, column.valueKind(), required);
            assertTrue(column.required(), required);
            assertEquals(DWHExcelNullHandling.KEEP_NULL, column.nullHandling(), required);
        }

        for (String correction : List.of("Year21", "Week21", "YearCorr", "WeekCorr")) {
            var column = definition.columns().stream()
                    .filter(candidate -> correction.equals(candidate.rawColumnName()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(DWHExcelValueKind.INT, column.valueKind(), correction);
            assertEquals(false, column.required(), correction);
            assertEquals(DWHExcelNullHandling.KEEP_NULL, column.nullHandling(), correction);
        }
    }

    @Test
    void javaProcessedWeeklyDataDoesNotDeclareRuntimeProcedure() {
        WeeklyDataExcelLoadDefinition definition = new WeeklyDataExcelLoadDefinition();

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                definition::processProcedureName
        );
        assertTrue(exception.getMessage().contains("WeeklyData processing is implemented in Java"));
    }
}
