package ru.stockmann.replenishment.services.dwhexcelload.definitions;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelValueKind;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDDataExcelLoadDefinitionTest {

    private static final Set<String> TEXT_COLUMNS = Set.of(
            "nazvanie",
            "sales_channel",
            "store_rus",
            "mfp_division",
            "mfp_department",
            "mfp_sub_department",
            "sku_brand_type",
            "sku_tm",
            "mfp_node",
            "section",
            "merchandise_sub_group",
            "campaign_sales",
            "sku_phase",
            "draivery_cd",
            "sku_color_rus",
            "sku_composition",
            "sku_supplier",
            "sku_name",
            "sku_collection",
            "sku_comment"
    );

    @Test
    void rawTextColumnsAllowValuesLongerThanTargetLength() {
        CDDataExcelLoadDefinition definition = new CDDataExcelLoadDefinition();

        definition.columns().forEach(column -> {
            if (TEXT_COLUMNS.contains(column.rawColumnName())) {
                assertEquals(4000, column.rawMaxLength(), column.rawColumnName());
                assertEquals(DWHExcelValueKind.TEXT, column.valueKind(), column.rawColumnName());
            }
        });
    }

    @Test
    void javaProcessedCdDataDoesNotDeclareRuntimeProcedure() {
        CDDataExcelLoadDefinition definition = new CDDataExcelLoadDefinition();

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                definition::processProcedureName
        );
        assertTrue(exception.getMessage().contains("CDData processing is implemented in Java"));
    }
}
