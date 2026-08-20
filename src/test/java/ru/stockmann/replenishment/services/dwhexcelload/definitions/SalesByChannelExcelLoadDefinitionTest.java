package ru.stockmann.replenishment.services.dwhexcelload.definitions;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelColumnSpec;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadType;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelNullHandling;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelValueKind;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SalesByChannelExcelLoadDefinitionTest {

    private final SalesByChannelExcelLoadDefinition definition = new SalesByChannelExcelLoadDefinition();

    @Test
    void registersSalesByChannelAndRawContract() {
        assertEquals(DWHExcelLoadType.SALES_BY_CHANNEL, definition.loadType());
        assertEquals(DWHExcelLoadType.SALES_BY_CHANNEL, DWHExcelLoadType.fromCode("SALES_BY_CHANNEL"));
        assertEquals("dbo.SalesByChannel_raw", definition.rawTableName());
        assertEquals("dbo.SalesByChannel", definition.targetTableName());
        assertEquals(29, definition.expectedColumnCount());
        assertEquals(10_000, definition.batchSize());
        assertThrows(UnsupportedOperationException.class, definition::processProcedureName);
    }

    @Test
    void declaresExactlyTheLiteralHeadersInOrder() {
        assertEquals(List.of(
                "Сезон_Year", "Сезон (6м)", "Year_month", "Year_сезон", "Year", "Month",
                "Sales Channel type", "StoreRUS", "TypeOfSales", "MFP Division", "MFP Department",
                "Campaign Sales Type", "Seasonality", "SKU Brand type", "Sum([Sales Quantity])",
                "Sum([Sales Curr])", "GM", "Discount TTL", "Sum([Turnover Curr])",
                "SKU SeasonBudget", "StoreRus_BPO", "Sales Channel_BPO", "MFP SubDepartment",
                "SKU TM", "MFP Node", "Section", "MerchandiseSubGroup", "SKU Phase",
                "SKU Product Class"
        ), definition.columns().stream().map(DWHExcelColumnSpec::excelColumnName).toList());
    }

    @Test
    void mapsHeadersToRawColumnsAndDeclaresTextDimensionsAndZeroMetrics() {
        assertEquals(List.of(
                "seasonYear", "season6m", "yearMonth", "yearSeason", "year", "month",
                "salesChannelType", "storeRus", "typeOfSales", "mfpDivision", "mfpDepartment",
                "campaignSalesType", "seasonality", "skuBrandType", "salesQuantity", "salesCurr",
                "gm", "discountTtl", "turnoverCurr", "skuSeasonBudget", "storeRusBpo",
                "salesChannelBpo", "mfpSubDepartment", "skuTm", "mfpNode", "section",
                "merchandiseSubGroup", "skuPhase", "skuProductClass"
        ), definition.columns().stream().map(DWHExcelColumnSpec::rawColumnName).toList());
        assertEquals(
                definition.columns().stream().map(DWHExcelColumnSpec::rawColumnName).toList(),
                definition.columns().stream().map(DWHExcelColumnSpec::targetColumnName).toList()
        );

        definition.columns().forEach(column -> {
            assertEquals(4000, column.rawMaxLength());
        });

        for (String name : List.of("salesQuantity", "salesCurr", "gm", "discountTtl", "turnoverCurr")) {
            DWHExcelColumnSpec column = definition.columns().stream()
                    .filter(candidate -> name.equals(candidate.rawColumnName()))
                    .findFirst()
                    .orElseThrow();
            assertEquals("salesQuantity".equals(name) ? DWHExcelValueKind.INT : DWHExcelValueKind.DECIMAL,
                    column.valueKind(), name);
            assertEquals(false, column.required(), name);
            assertEquals(DWHExcelNullHandling.ZERO, column.nullHandling(), name);
        }

        definition.columns().stream()
                .filter(column -> !List.of(
                        "salesQuantity", "salesCurr", "gm", "discountTtl", "turnoverCurr"
                ).contains(column.rawColumnName()))
                .forEach(column -> {
                    assertEquals(DWHExcelValueKind.TEXT, column.valueKind(), column.rawColumnName());
                    assertEquals(DWHExcelNullHandling.KEEP_NULL, column.nullHandling(), column.rawColumnName());
                    assertEquals(
                            "year".equals(column.rawColumnName()) || "month".equals(column.rawColumnName()),
                            column.required(), column.rawColumnName()
                    );
                });
    }
}
