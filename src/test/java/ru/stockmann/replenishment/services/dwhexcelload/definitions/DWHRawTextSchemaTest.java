package ru.stockmann.replenishment.services.dwhexcelload.definitions;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelColumnSpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DWHRawTextSchemaTest {

    private static final List<String> WEEKLY_TEXT_COLUMNS = List.of(
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

    private static final List<String> CD_DATA_TEXT_COLUMNS = List.of(
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
    void weeklyDataMainDdlUsesWideRawTextAndKeepsTargetTextLength() throws Exception {
        String ddl = normalizedSql("src/main/db/tables/Weekly_data_ddl.sql");
        WeeklyDataExcelLoadDefinition definition = new WeeklyDataExcelLoadDefinition();

        for (String column : WEEKLY_TEXT_COLUMNS) {
            assertColumnDefinition(ddl, column, "nvarchar(255)", "target");
            assertColumnDefinition(ddl, column, "nvarchar(4000)", "raw");
            assertRawMaxLength(definition.columns(), column);
        }
    }

    @Test
    void cdDataMainDdlUsesWideRawTextAndKeepsTargetTextLength() throws Exception {
        String ddl = normalizedSql("src/main/db/tables/CDdata_ddl.sql");
        CDDataExcelLoadDefinition definition = new CDDataExcelLoadDefinition();

        for (String column : CD_DATA_TEXT_COLUMNS) {
            if ("nazvanie".equals(column)) {
                assertNotNullColumnDefinition(ddl, column, "nvarchar(255)", "target");
            } else {
                assertColumnDefinition(ddl, column, "nvarchar(255)", "target");
            }
            assertColumnDefinition(ddl, column, "nvarchar(4000)", "raw");
            assertRawMaxLength(definition.columns(), column);
        }
    }

    private static String normalizedSql(String path) throws Exception {
        return Files.readString(Path.of(path))
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private static void assertColumnDefinition(String sql, String column, String type, String context) {
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(column.toLowerCase(Locale.ROOT))
                + "\\s+" + Pattern.quote(type) + "\\s+null\\b");
        assertTrue(pattern.matcher(sql).find(), context + " column " + column + " should be " + type + " NULL");
    }

    private static void assertNotNullColumnDefinition(
            String sql,
            String column,
            String type,
            String context
    ) {
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(column.toLowerCase(Locale.ROOT))
                + "\\s+" + Pattern.quote(type) + "\\s+not null\\b");
        assertTrue(pattern.matcher(sql).find(),
                context + " column " + column + " should be " + type + " NOT NULL");
    }

    private static void assertRawMaxLength(List<DWHExcelColumnSpec> columns, String rawColumnName) {
        DWHExcelColumnSpec column = columns.stream()
                .filter(c -> rawColumnName.equals(c.rawColumnName()))
                .findFirst()
                .orElseThrow();
        assertTrue(column.rawMaxLength() == 4000, "Definition rawMaxLength should be 4000 for " + rawColumnName);
    }
}
