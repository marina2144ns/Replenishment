package ru.stockmann.replenishment.services.dwhexcelload.definitions;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelColumnSpec;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadType;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelNullHandling;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CDEcomExcelLoadDefinitionTest {

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
        definition.columns().forEach(column ->
                assertEquals(DWHExcelNullHandling.KEEP_NULL, column.nullHandling())
        );
    }
}
