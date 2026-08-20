package ru.stockmann.replenishment.services.dwhexcelload.definitions;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelColumnSpec;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadType;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelNullHandling;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelValueKind;

import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CDEcomExcelLoadDefinitionTest {

    private static final Set<String> ZERO_METRICS = Set.of(
            "orderPcs", "orderRub", "foundPcs", "foundRub", "salesPcs", "salesRub",
            "revenue", "gp", "cogs", "salesDiscount", "planRub", "stockStoresPcs",
            "stockStoresDdp"
    );

    @Test
    void definitionUsesCdecomLoadTypeAndTables() {
        CDEcomExcelLoadDefinition definition = new CDEcomExcelLoadDefinition();

        assertEquals(DWHExcelLoadType.CD_ECOM, definition.loadType());
        assertEquals("dbo.CD_ecom_raw", definition.rawTableName());
        assertEquals("dbo.CD_ecom", definition.targetTableName());
        assertEquals(38, definition.expectedColumnCount());
        assertEquals(10_000, definition.batchSize());
    }

    @Test
    void javaProcessedCdecomDoesNotDeclareRuntimeProcedure() {
        CDEcomExcelLoadDefinition definition = new CDEcomExcelLoadDefinition();

        assertThrows(UnsupportedOperationException.class, definition::processProcedureName);
    }

    @Test
    void columnsFollowCdecomRawTableOrder() {
        CDEcomExcelLoadDefinition definition = new CDEcomExcelLoadDefinition();

        List<String> columns = definition.columns().stream()
                .map(DWHExcelColumnSpec::rawColumnName)
                .toList();

        assertEquals(List.of(
                "name",
                "year",
                "season",
                "day",
                "data",
                "salesChannelBpo",
                "storeRus",
                "mfpDivision",
                "mfpDepartment",
                "mfpSubDepartment",
                "skuBrandType",
                "skuTm",
                "mfpNode",
                "section",
                "merchandiseSubGroup",
                "campaignSalesType",
                "skuStyleColor",
                "skuPhase",
                "orderPcs",
                "orderRub",
                "foundPcs",
                "foundRub",
                "salesPcs",
                "salesRub",
                "revenue",
                "gp",
                "cogs",
                "salesDiscount",
                "planRub",
                "stockStoresPcs",
                "stockStoresDdp",
                "cdDrivers",
                "skuSupplierModel",
                "skuComposition",
                "skuColorRussian",
                "skuName",
                "skuCommentBuyer",
                "skuCollection"
        ), columns);
        assertEquals(columns, definition.columns().stream()
                .map(DWHExcelColumnSpec::targetColumnName)
                .toList());
        assertEquals(List.of(
                "название", "ГОД", "Сезон", "день", "дата", "Sales Channel_BPO", "StoreRUS",
                "MFP Division", "MFP Department", "MFP SubDepartment", "SKU Brand type", "SKU TM",
                "MFP Node", "Section", "Merchandise SubGroup", "Campaign Sales Type", "SKU StyleColor",
                "SKU Phase", "Заказ, шт", "Заказ, руб", "Найдено,шт", "Найдено,руб", "Sales, Pcs",
                "Sales, rub", "Revenue", "GP", "Cogs", "Sales Discount", "Plan, rub",
                "Stock Stores, Pcs", "Stock Stores, DDP", "Драйверы CD", "SKU Supplier model",
                "SKU Composition", "SKU Color Russian", "SKU Name", "SKU Comment (buyer)",
                "SKU Collection"
        ), definition.columns().stream().map(DWHExcelColumnSpec::excelColumnName).toList());
    }

    @Test
    void columnLengthsAndNullHandlingMatchRawSchema() {
        CDEcomExcelLoadDefinition definition = new CDEcomExcelLoadDefinition();

        assertEquals(4000, definition.columns().get(0).rawMaxLength());
        assertEquals(50, definition.columns().get(1).rawMaxLength());
        assertEquals(50, definition.columns().get(4).rawMaxLength());
        assertEquals(100, definition.columns().get(16).rawMaxLength());
        assertEquals(100, definition.columns().get(30).rawMaxLength());
        assertEquals(4000, definition.columns().get(37).rawMaxLength());
    }

    @Test
    void declaresRequiredKeysOptionalZeroMetricsAndNullableIdentifierExplicitly() {
        CDEcomExcelLoadDefinition definition = new CDEcomExcelLoadDefinition();

        definition.columns().stream()
                .filter(column -> ZERO_METRICS.contains(column.rawColumnName()))
                .forEach(column -> {
                    DWHExcelValueKind expected = Set.of("planRub", "stockStoresPcs", "stockStoresDdp")
                            .contains(column.rawColumnName())
                            ? DWHExcelValueKind.INT
                            : DWHExcelValueKind.DECIMAL;
                    assertEquals(expected, column.valueKind(), column.rawColumnName());
                    assertEquals(false, column.required(), column.rawColumnName());
                    assertEquals(DWHExcelNullHandling.ZERO, column.nullHandling(), column.rawColumnName());
                });
        assertEquals(13, definition.columns().stream()
                .filter(column -> ZERO_METRICS.contains(column.rawColumnName()))
                .count());

        for (String name : List.of("name", "year", "season", "day")) {
            DWHExcelColumnSpec column = definition.columns().stream()
                    .filter(candidate -> name.equals(candidate.rawColumnName()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(true, column.required(), name);
            assertEquals(DWHExcelNullHandling.KEEP_NULL, column.nullHandling(), name);
        }

        DWHExcelColumnSpec identifier = definition.columns().stream()
                .filter(column -> "skuStyleColor".equals(column.rawColumnName()))
                .findFirst()
                .orElseThrow();
        assertEquals(DWHExcelValueKind.DECIMAL, identifier.valueKind());
        assertEquals(false, identifier.required());
        assertEquals(DWHExcelNullHandling.KEEP_NULL, identifier.nullHandling());
    }

    @Test
    void numericExcelDateSerialIsTimezoneIndependentForProjectZones() {
        CDEcomExcelLoadDefinition definition = new CDEcomExcelLoadDefinition();
        DWHExcelColumnSpec dataColumn = definition.columns().get(4);
        TimeZone originalTimeZone = TimeZone.getDefault();

        try {
            assertEquals("01.01.2025", normalizeInTimeZone(dataColumn, "45658", "Europe/Belgrade"));
            assertEquals("01.01.2025", normalizeInTimeZone(dataColumn, "45658", "Europe/Moscow"));
            assertEquals("01.01.2025", normalizeInTimeZone(dataColumn, "45658", "UTC"));
            assertEquals("01.01.2025", normalizeInTimeZone(dataColumn, "45658.75", "Europe/Belgrade"));
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }

        assertEquals(originalTimeZone, TimeZone.getDefault());
    }

    private static String normalizeInTimeZone(DWHExcelColumnSpec column, String value, String timeZoneId) {
        TimeZone.setDefault(TimeZone.getTimeZone(timeZoneId));
        return column.normalizer().normalize(value);
    }
}
