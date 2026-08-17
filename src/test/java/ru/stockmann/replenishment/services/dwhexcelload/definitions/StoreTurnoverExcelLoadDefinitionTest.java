package ru.stockmann.replenishment.services.dwhexcelload.definitions;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.core.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StoreTurnoverExcelLoadDefinitionTest {
    private final StoreTurnoverExcelLoadDefinition definition = new StoreTurnoverExcelLoadDefinition();

    @Test void exactCsvSchemaAndMetadataAreCanonical() {
        assertEquals(DWHExcelLoadType.STORE_TURNOVER, definition.loadType());
        assertEquals("STORE_TURNOVER", definition.loadCode());
        assertEquals("dbo.StoreTurnover_raw", definition.rawTableName());
        assertEquals(11, definition.expectedColumnCount());
        assertEquals(List.of("Sku","Period","StoreRus","RemainingSum","RemainingDays","SalesQuantity","Sales","Asp","Revenue","Gp","DiscountTotal"),
                definition.columns().stream().map(DWHExcelColumnSpec::excelColumnName).toList());
        assertEquals(List.of(0,1,2,3,4,5,6,7,8,9,10), definition.columns().stream().map(DWHExcelColumnSpec::excelIndex).toList());
    }

    @Test void requiredFieldsAndZeroMetricsMatchInputContract() {
        assertTrue(definition.columns().get(0).required());
        assertTrue(definition.columns().get(1).required());
        assertTrue(definition.columns().get(2).required());
        assertEquals(DWHExcelValueKind.TEXT, definition.columns().get(0).valueKind());
        assertEquals(DWHExcelValueKind.DATE, definition.columns().get(1).valueKind());
        for (DWHExcelColumnSpec metric : definition.columns().subList(3,11)) {
            assertFalse(metric.required());
            assertEquals(DWHExcelValueKind.INT, metric.valueKind());
            assertEquals(DWHExcelNullHandling.ZERO, metric.nullHandling());
        }
    }
}
