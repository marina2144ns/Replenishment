package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.businessColumns;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.insertColumns;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.names;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.normalizeSql;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.placeholderCount;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.read;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.tableColumns;

class CDDataStageSchemaContractTest {

    private static final String CD_DATA_DDL = "src/main/db/tables/CDdata_ddl.sql";
    private static final String USERS_DDL = "src/main/db/tables/Users.example.sql";
    private static final String TARGET_TABLE = "dbo.CD_data";
    private static final String STAGE_TABLE = "dbo.CD_data_stage";
    private static final String STAGE_REPOSITORY =
            "src/main/java/ru/stockmann/replenishment/services/cddata/process/CDDataStageRepository.java";
    private static final String PROCESSOR =
            "src/main/java/ru/stockmann/replenishment/services/cddata/process/CDDataProcessor.java";
    private static final String REQUIRED_FIELDS_MIGRATION =
            "src/main/db/tables/cddata_required_delete_fields_migration.sql";
    private static final String ZERO_METRICS_MIGRATION =
            "src/main/db/tables/cddata_zero_metrics_migration.sql";

    private static final List<String> BUSINESS_COLUMNS = List.of(
            "nazvanie",
            "god",
            "sezon",
            "den",
            "data",
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
            "sku_style_color",
            "sku_phase",
            "stock_start_pcs",
            "stock_start_dd",
            "sales_pcs",
            "sales_rub",
            "revenue",
            "gp",
            "cogs",
            "sales_frp_price",
            "sales_discount",
            "stock_stores_pcs",
            "stock_stores_dd",
            "plan_rub",
            "draivery_cd",
            "sku_color_rus",
            "sku_composition",
            "sku_supplier",
            "sku_name",
            "sku_collection",
            "sku_comment",
            "rawrowid"
    );

    @Test
    void stageTableHasCompleteOrderedTypedContract() throws Exception {
        List<DWHSchemaTestSupport.ColumnDef> columns = tableColumns(CD_DATA_DDL, STAGE_TABLE);

        assertEquals(concat(List.of("loadsessionid", "excelrownum"), BUSINESS_COLUMNS), names(columns));
        assertColumn(columns, "loadsessionid", "bigint", false);
        assertColumn(columns, "excelrownum", "bigint", true);

        for (String name : List.of("god", "sezon", "den")) {
            assertColumn(columns, name, "int", false);
        }
        assertColumn(columns, "nazvanie", "nvarchar(255)", false);
        assertColumn(columns, "plan_rub", "int", false);
        assertColumn(columns, "data", "date", true);
        assertColumn(columns, "sku_style_color", "bigint", true);
        assertColumn(columns, "rawrowid", "bigint", true);

        for (String name : List.of(
                "stock_start_pcs",
                "stock_start_dd",
                "sales_pcs",
                "sales_rub",
                "revenue",
                "gp",
                "cogs",
                "sales_frp_price",
                "sales_discount",
                "stock_stores_pcs",
                "stock_stores_dd"
        )) {
            assertColumn(columns, name, "decimal(18,2)", false);
        }

        for (String name : List.of(
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
        )) {
            assertColumn(columns, name, "nvarchar(255)", true);
        }
    }

    @Test
    void targetDeleteFieldsAreNotNullable() throws Exception {
        List<DWHSchemaTestSupport.ColumnDef> target = tableColumns(CD_DATA_DDL, TARGET_TABLE);

        assertColumn(target, "nazvanie", "nvarchar(255)", false);
        assertColumn(target, "god", "int", false);
        assertColumn(target, "sezon", "int", false);
        assertColumn(target, "den", "int", false);
    }

    @Test
    void zeroMetricsAreNotNullableAndMatchTargetContract() throws Exception {
        List<DWHSchemaTestSupport.ColumnDef> target = tableColumns(CD_DATA_DDL, TARGET_TABLE);
        List<DWHSchemaTestSupport.ColumnDef> stage = tableColumns(CD_DATA_DDL, STAGE_TABLE);

        for (String name : decimalMetrics()) {
            assertColumn(stage, name, "decimal(18,2)", false);
            assertColumn(target, name, "decimal(18,2)", false);
        }
        assertColumn(stage, "plan_rub", "int", false);
        assertColumn(target, "plan_rub", "int", false);
    }

    @Test
    void stageBusinessColumnsMatchTargetTypesAndNullability() throws Exception {
        List<DWHSchemaTestSupport.ColumnDef> target = tableColumns(CD_DATA_DDL, TARGET_TABLE);
        List<DWHSchemaTestSupport.ColumnDef> stage = tableColumns(CD_DATA_DDL, STAGE_TABLE);

        assertEquals(BUSINESS_COLUMNS, businessColumns(target));
        assertEquals(BUSINESS_COLUMNS, businessColumns(stage));

        for (String name : BUSINESS_COLUMNS) {
            DWHSchemaTestSupport.ColumnDef targetColumn = column(target, name);
            DWHSchemaTestSupport.ColumnDef stageColumn = column(stage, name);
            assertEquals(targetColumn.type(), stageColumn.type(), name);
            assertEquals(isNullable(targetColumn), isNullable(stageColumn), name);
        }

        assertFalse(names(stage).contains("id"));
        assertFalse(names(stage).contains("createdat"));
        assertTrue(column(target, "createdat").contains("default sysdatetime()"));
    }

    @Test
    void stageHasOnlyLoadSessionForeignKeyAndIndexWithoutCascadeOrDrop() throws Exception {
        String ddl = normalizeSql(read(CD_DATA_DDL));

        assertTrue(ddl.contains(
                "constraint fk_cd_data_stage_load_session foreign key (loadsessionid) "
                        + "references dbo.dwh_excel_load_session(id)"
        ));
        assertFalse(ddl.contains(
                "fk_cd_data_stage_load_session foreign key (loadsessionid) "
                        + "references dbo.dwh_excel_load_session(id) on delete cascade"
        ));
        assertTrue(ddl.contains(
                "create index ix_cd_data_stage_loadsessionid "
                        + "on dbo.cd_data_stage(loadsessionid)"
        ));
        assertFalse(ddl.contains("drop table dbo.cd_data_stage"));
    }

    @Test
    void stageUsesTypedTargetValuesInsteadOfRawTextRepresentations() throws Exception {
        List<DWHSchemaTestSupport.ColumnDef> stage = tableColumns(CD_DATA_DDL, STAGE_TABLE);

        for (String name : List.of(
                "god",
                "sezon",
                "den",
                "data",
                "sku_style_color",
                "stock_start_pcs",
                "stock_start_dd",
                "sales_pcs",
                "sales_rub",
                "revenue",
                "gp",
                "cogs",
                "sales_frp_price",
                "sales_discount",
                "stock_stores_pcs",
                "stock_stores_dd",
                "plan_rub"
        )) {
            assertFalse(column(stage, name).type().startsWith("nvarchar"), name);
        }
        assertTrue(stage.stream().noneMatch(c -> c.type().equals("nvarchar(4000)")));
        assertTrue(stage.stream().noneMatch(c -> c.type().equals("nvarchar(50)")));
    }

    @Test
    void applicationUserHasStageDmlPermissionsWithoutDdlPrivileges() throws Exception {
        String permissions = normalizeSql(read(USERS_DDL));

        assertTrue(permissions.contains(
                "grant select, insert, update, delete "
                        + "on object::dbo.cd_data_stage to repl_service"
        ));
        assertFalse(permissions.contains(
                "grant alter on object::dbo.cd_data_stage to repl_service"
        ));
        assertFalse(permissions.contains(
                "grant control on object::dbo.cd_data_stage to repl_service"
        ));
    }

    @Test
    void stageRepositoryInsertContainsEveryColumnInExactOrder() throws Exception {
        List<String> columns = names(tableColumns(CD_DATA_DDL, STAGE_TABLE));

        assertEquals(columns, insertColumns(STAGE_REPOSITORY, STAGE_TABLE));
        assertEquals(columns.size(), placeholderCount(STAGE_REPOSITORY, STAGE_TABLE));
    }

    @Test
    void processorUsesChunkStagePathAndSetBasedPublishWithoutStoredProcedureOrTargetBatch() throws Exception {
        String processor = normalizeSql(read(PROCESSOR));

        assertTrue(processor.contains("rawrepository.findchunk(loadsessionid, lastrawid)"));
        assertTrue(processor.contains("stagerepository.insertbatch"));
        assertTrue(processor.contains("targetrepository.publishfromstage"));
        assertFalse(processor.contains("findbyloadsessionid"));
        assertFalse(processor.contains("targetrepository.insertall"));
        assertFalse(processor.contains("processprocedurename"));
    }

    @Test
    void requiredDeleteFieldsMigrationGuardsColumnChangesAndDependentIndex() throws Exception {
        String migration = normalizeSql(read(REQUIRED_FIELDS_MIGRATION));

        assertTrue(migration.contains("from dbo.cd_data where god is null"));
        assertTrue(migration.contains("from dbo.cd_data_stage where god is null"));
        assertTrue(migration.contains("nazvanie is null"));
        assertTrue(migration.contains("nchar(160)"));
        assertTrue(migration.contains("nchar(8239)"));
        assertTrue(migration.contains("throw 50001"));
        assertTrue(migration.contains("legacy target or stage rows violate the required-field contract"));

        for (String table : List.of("dbo.cd_data", "dbo.cd_data_stage")) {
            assertTrue(migration.contains(
                    "alter table " + table + " alter column nazvanie nvarchar(255) not null"
            ));
            for (String field : List.of("god", "sezon", "den")) {
                assertTrue(migration.contains(
                        "alter table " + table + " alter column " + field + " int not null"
                ));
            }
        }

        assertEquals(8, occurrences(migration, "alter table dbo.cd_data"));
        assertTrue(migration.contains("name in (n'god', n'sezon') and is_nullable = 1"));
        assertEquals(1, occurrences(migration, "drop index ix_cd_data_god_sezon"));
        assertEquals(1, occurrences(migration, "create nonclustered index ix_cd_data_god_sezon"));
        assertFalse(migration.contains("drop table"));
        assertFalse(migration.contains("delete from"));
        assertFalse(migration.contains("update "));
        assertFalse(migration.contains("truncate"));
        assertFalse(migration.contains("default"));
    }

    @Test
    void zeroMetricsMigrationUpdatesOnlyMetricNullsThenGuardsNotNullAlter() throws Exception {
        String migration = normalizeSql(read(ZERO_METRICS_MIGRATION));

        for (String table : List.of("dbo.cd_data", "dbo.cd_data_stage")) {
            for (String field : decimalMetrics()) {
                assertTrue(migration.contains(
                        "update " + table + " set " + field + " = 0 where " + field + " is null"
                ), table + "." + field);
                assertTrue(migration.contains(
                        "alter table " + table + " alter column " + field
                                + " decimal(18,2) not null"
                ), table + "." + field);
            }
            assertTrue(migration.contains(
                    "update " + table + " set plan_rub = 0 where plan_rub is null"
            ));
            assertTrue(migration.contains(
                    "alter table " + table + " alter column plan_rub int not null"
            ));
        }

        assertEquals(24, occurrences(migration, "is_nullable = 1"));
        assertFalse(migration.contains("cd_data_raw"));
        for (String field : List.of("god", "sezon", "nazvanie", "den", "sku_style_color")) {
            assertFalse(migration.contains("alter column " + field + " "), field);
        }
        assertFalse(migration.contains("delete "));
        assertFalse(migration.contains("drop "));
        assertFalse(migration.contains("truncate "));
        assertFalse(migration.contains("default"));
    }

    private static List<String> decimalMetrics() {
        return List.of(
                "stock_start_pcs", "stock_start_dd", "sales_pcs", "sales_rub", "revenue", "gp",
                "cogs", "sales_frp_price", "sales_discount", "stock_stores_pcs", "stock_stores_dd"
        );
    }

    private static List<String> concat(List<String> first, List<String> second) {
        return java.util.stream.Stream.concat(first.stream(), second.stream()).toList();
    }

    private static int occurrences(String value, String token) {
        return (value.length() - value.replace(token, "").length()) / token.length();
    }

    private static DWHSchemaTestSupport.ColumnDef column(
            List<DWHSchemaTestSupport.ColumnDef> columns,
            String name
    ) {
        return columns.stream()
                .filter(candidate -> candidate.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static boolean isNullable(DWHSchemaTestSupport.ColumnDef column) {
        return column.contains(column.type() + " null")
                && !column.contains(column.type() + " not null");
    }

    private static void assertColumn(
            List<DWHSchemaTestSupport.ColumnDef> columns,
            String name,
            String type,
            boolean nullable
    ) {
        DWHSchemaTestSupport.ColumnDef column = column(columns, name);

        assertEquals(type, column.type(), name);
        if (nullable) {
            assertTrue(isNullable(column), name + " should be nullable");
        } else {
            assertTrue(column.contains(type + " not null"), name + " should be NOT NULL");
        }
    }
}
