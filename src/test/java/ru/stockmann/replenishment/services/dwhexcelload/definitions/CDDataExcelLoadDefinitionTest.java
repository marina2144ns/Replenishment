package ru.stockmann.replenishment.services.dwhexcelload.definitions;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelValueKind;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelNullHandling;

import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDDataExcelLoadDefinitionTest {

    private static final Set<String> ZERO_METRICS = Set.of(
            "stock_start_pcs", "stock_start_dd", "sales_pcs", "sales_rub", "revenue", "gp",
            "cogs", "sales_frp_price", "sales_discount", "stock_stores_pcs", "stock_stores_dd",
            "plan_rub"
    );

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
    void declaresExactHeaderContractAndColumnCount() {
        CDDataExcelLoadDefinition definition = new CDDataExcelLoadDefinition();

        assertEquals(37, definition.expectedColumnCount());
        assertEquals(List.of(
                "nazvanie", "god", "sezon", "den", "data", "sales_channel", "store_rus",
                "mfp_division", "mfp_department", "mfp_sub_department", "sku_brand_type",
                "sku_tm", "mfp_node", "section", "merchandise_sub_group", "campaign_sales",
                "sku_style_color", "sku_phase", "stock_start_pcs", "stock_start_dd",
                "sales_pcs", "sales_rub", "revenue", "gp", "cogs", "sales_frp_price",
                "sales_discount", "stock_stores_pcs", "stock_stores_dd", "plan_rub",
                "draivery_cd", "sku_color_rus", "sku_composition", "sku_supplier",
                "sku_name", "sku_collection", "sku_comment"
        ), definition.columns().stream().map(column -> column.excelColumnName()).toList());
    }

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
    void declaresRequiredDeleteKeysAndOptionalZeroMetricsExplicitly() {
        CDDataExcelLoadDefinition definition = new CDDataExcelLoadDefinition();

        definition.columns().stream()
                .filter(column -> ZERO_METRICS.contains(column.rawColumnName()))
                .forEach(column -> {
                    DWHExcelValueKind expectedKind = "plan_rub".equals(column.rawColumnName())
                            ? DWHExcelValueKind.INT
                            : DWHExcelValueKind.DECIMAL;
                    assertEquals(expectedKind, column.valueKind(), column.rawColumnName());
                    assertEquals(false, column.required(), column.rawColumnName());
                    assertEquals(DWHExcelNullHandling.ZERO, column.nullHandling(), column.rawColumnName());
                });
        assertEquals(12, definition.columns().stream()
                .filter(column -> ZERO_METRICS.contains(column.rawColumnName()))
                .count());

        for (String name : List.of("nazvanie", "god", "sezon", "den")) {
            var column = definition.columns().stream()
                    .filter(candidate -> name.equals(candidate.rawColumnName()))
                    .findFirst()
                    .orElseThrow();
            assertTrue(column.required(), name);
            assertEquals(DWHExcelNullHandling.KEEP_NULL, column.nullHandling(), name);
        }

        var skuStyleColor = definition.columns().stream()
                .filter(column -> "sku_style_color".equals(column.rawColumnName()))
                .findFirst()
                .orElseThrow();
        assertEquals(DWHExcelValueKind.INT, skuStyleColor.valueKind());
        assertEquals(false, skuStyleColor.required());
        assertEquals(DWHExcelNullHandling.KEEP_NULL, skuStyleColor.nullHandling());
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
