package ru.stockmann.replenishment.services.dwhexcelload.definitions;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelValueKind;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyDataExcelLoadDefinitionTest {

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
    void javaProcessedWeeklyDataDoesNotDeclareRuntimeProcedure() {
        WeeklyDataExcelLoadDefinition definition = new WeeklyDataExcelLoadDefinition();

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                definition::processProcedureName
        );
        assertTrue(exception.getMessage().contains("WeeklyData processing is implemented in Java"));
    }
}
