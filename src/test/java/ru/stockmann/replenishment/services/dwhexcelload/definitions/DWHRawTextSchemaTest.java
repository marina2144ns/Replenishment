package ru.stockmann.replenishment.services.dwhexcelload.definitions;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelColumnSpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void weeklyDataMainDdlAndMigrationUseWideRawTextAndKeepTargetTextLength() throws Exception {
        String ddl = normalizedSql("src/main/db/tables/Weekly_data_ddl.sql");
        String migration = normalizedSql("src/main/db/tables/Weekly_data_raw_text_migration.sql");
        WeeklyDataExcelLoadDefinition definition = new WeeklyDataExcelLoadDefinition();

        for (String column : WEEKLY_TEXT_COLUMNS) {
            assertColumnDefinition(ddl, column, "nvarchar(255)", "target");
            assertColumnDefinition(ddl, column, "nvarchar(4000)", "raw");
            assertAlterColumn(migration, "dbo.weekly_data_raw", column);
            assertRawMaxLength(definition.columns(), column);
        }

        assertFalse(migration.contains("totalstockpcs nvarchar(4000)"));
        assertFalse(migration.contains("totalstockddp nvarchar(4000)"));
        assertFalse(migration.contains("salespcs nvarchar(4000)"));
        assertFalse(migration.contains("salesrub nvarchar(4000)"));
        assertFalse(migration.contains("revenue nvarchar(4000)"));
        assertFalse(migration.contains("gp nvarchar(4000)"));
        assertFalse(migration.contains("discounttotalrub nvarchar(4000)"));
    }

    @Test
    void cdDataMainDdlAndMigrationUseWideRawTextAndKeepTargetTextLength() throws Exception {
        String ddl = normalizedSql("src/main/db/tables/CDdata_ddl.sql");
        String migration = normalizedSql("src/main/db/tables/CDdata_raw_text_migration.sql");
        CDDataExcelLoadDefinition definition = new CDDataExcelLoadDefinition();

        for (String column : CD_DATA_TEXT_COLUMNS) {
            assertColumnDefinition(ddl, column, "nvarchar(255)", "target");
            assertColumnDefinition(ddl, column, "nvarchar(4000)", "raw");
            assertAlterColumn(migration, "dbo.cd_data_raw", column);
            assertRawMaxLength(definition.columns(), column);
        }

        assertFalse(migration.contains("stock_start_pcs nvarchar(4000)"));
        assertFalse(migration.contains("stock_start_dd nvarchar(4000)"));
        assertFalse(migration.contains("sales_pcs nvarchar(4000)"));
        assertFalse(migration.contains("sales_rub nvarchar(4000)"));
        assertFalse(migration.contains("revenue nvarchar(4000)"));
        assertFalse(migration.contains("gp nvarchar(4000)"));
        assertFalse(migration.contains("cogs nvarchar(4000)"));
        assertFalse(migration.contains("sales_frp_price nvarchar(4000)"));
        assertFalse(migration.contains("sales_discount nvarchar(4000)"));
        assertFalse(migration.contains("stock_stores_pcs nvarchar(4000)"));
        assertFalse(migration.contains("stock_stores_dd nvarchar(4000)"));
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

    private static void assertAlterColumn(String migration, String tableName, String column) {
        String expected = "alter table " + tableName + " alter column "
                + column.toLowerCase(Locale.ROOT) + " nvarchar(4000) null";
        assertTrue(migration.contains(expected), "Migration should widen raw column " + column);
    }

    private static void assertRawMaxLength(List<DWHExcelColumnSpec> columns, String rawColumnName) {
        DWHExcelColumnSpec column = columns.stream()
                .filter(c -> rawColumnName.equals(c.rawColumnName()))
                .findFirst()
                .orElseThrow();
        assertTrue(column.rawMaxLength() == 4000, "Definition rawMaxLength should be 4000 for " + rawColumnName);
    }
}
