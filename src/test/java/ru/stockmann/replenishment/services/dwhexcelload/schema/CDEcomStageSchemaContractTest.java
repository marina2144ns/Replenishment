package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.businessColumns;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.names;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.normalizeSql;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.read;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.tableColumns;

class CDEcomStageSchemaContractTest {

    private static final String DDL = "src/main/db/tables/CDecom_ddl.sql";
    private static final String USERS_DDL = "src/main/db/tables/Users.example.sql";
    private static final String TARGET_TABLE = "dbo.CD_ecom";
    private static final String STAGE_TABLE = "dbo.CD_ecom_stage";
    private static final String REQUIRED_FIELDS_MIGRATION =
            "src/main/db/tables/cdecom_required_delete_fields_migration.sql";

    private static final List<String> BUSINESS_COLUMNS = List.of(
            "name",
            "year",
            "season",
            "day",
            "data",
            "saleschannelbpo",
            "storerus",
            "mfpdivision",
            "mfpdepartment",
            "mfpsubdepartment",
            "skubrandtype",
            "skutm",
            "mfpnode",
            "section",
            "merchandisesubgroup",
            "campaignsalestype",
            "skustylecolor",
            "skuphase",
            "orderpcs",
            "orderrub",
            "foundpcs",
            "foundrub",
            "salespcs",
            "salesrub",
            "revenue",
            "gp",
            "cogs",
            "salesdiscount",
            "planrub",
            "stockstorespcs",
            "stockstoresddp",
            "cddrivers",
            "skusuppliermodel",
            "skucomposition",
            "skucolorrussian",
            "skuname",
            "skucommentbuyer",
            "skucollection",
            "rawrowid"
    );

    @Test
    void stageHasCompleteOrderedTypedContract() throws Exception {
        List<DWHSchemaTestSupport.ColumnDef> columns = tableColumns(DDL, STAGE_TABLE);

        assertEquals(concat(List.of("loadsessionid", "excelrownum"), BUSINESS_COLUMNS), names(columns));
        assertColumn(columns, "loadsessionid", "bigint", false);
        assertColumn(columns, "excelrownum", "bigint", true);

        for (String name : List.of("year", "season", "day")) {
            assertColumn(columns, name, "int", false);
        }
        assertColumn(columns, "name", "nvarchar(255)", false);
        assertColumn(columns, "data", "date", true);
        assertColumn(columns, "rawrowid", "bigint", true);

        for (String name : List.of(
                "skustylecolor", "planrub", "stockstorespcs", "stockstoresddp"
        )) {
            assertColumn(columns, name, "bigint", true);
        }

        for (String name : List.of(
                "orderpcs", "orderrub", "foundpcs", "foundrub", "salespcs",
                "salesrub", "revenue", "gp", "cogs", "salesdiscount"
        )) {
            assertColumn(columns, name, "decimal(18,2)", true);
        }

        for (String name : List.of(
                "saleschannelbpo", "storerus", "mfpdivision", "mfpdepartment",
                "mfpsubdepartment", "skubrandtype", "skutm", "mfpnode", "section",
                "merchandisesubgroup", "campaignsalestype", "skuphase", "cddrivers",
                "skusuppliermodel", "skucomposition", "skucolorrussian", "skuname",
                "skucommentbuyer", "skucollection"
        )) {
            assertColumn(columns, name, "nvarchar(255)", true);
        }
    }

    @Test
    void targetDeleteFieldsAreNotNullable() throws Exception {
        List<DWHSchemaTestSupport.ColumnDef> target = tableColumns(DDL, TARGET_TABLE);

        assertColumn(target, "name", "nvarchar(255)", false);
        assertColumn(target, "year", "int", false);
        assertColumn(target, "season", "int", false);
        assertColumn(target, "day", "int", false);
    }

    @Test
    void stageBusinessColumnsMatchTargetTypesAndNullability() throws Exception {
        List<DWHSchemaTestSupport.ColumnDef> target = tableColumns(DDL, TARGET_TABLE);
        List<DWHSchemaTestSupport.ColumnDef> stage = tableColumns(DDL, STAGE_TABLE);

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
        assertTrue(column(target, "createdat").contains("default sysutcdatetime()"));
    }

    @Test
    void dateColumnIsTimezoneIndependentSqlDate() throws Exception {
        DWHSchemaTestSupport.ColumnDef date = column(tableColumns(DDL, STAGE_TABLE), "data");

        assertEquals("date", date.type());
        assertFalse(date.type().startsWith("datetime"));
        assertFalse(date.type().startsWith("datetimeoffset"));
    }

    @Test
    void stageUsesTypedValuesInsteadOfRawTextRepresentations() throws Exception {
        List<DWHSchemaTestSupport.ColumnDef> stage = tableColumns(DDL, STAGE_TABLE);

        for (String name : List.of(
                "year", "season", "day", "data", "skustylecolor",
                "orderpcs", "orderrub", "foundpcs", "foundrub", "salespcs",
                "salesrub", "revenue", "gp", "cogs", "salesdiscount",
                "planrub", "stockstorespcs", "stockstoresddp"
        )) {
            assertFalse(column(stage, name).type().startsWith("nvarchar"), name);
        }
        assertTrue(stage.stream().noneMatch(c -> c.type().equals("nvarchar(4000)")));
        assertTrue(stage.stream().noneMatch(c -> c.type().equals("nvarchar(100)")));
        assertTrue(stage.stream().noneMatch(c -> c.type().equals("nvarchar(50)")));
    }

    @Test
    void stageHasLoadSessionForeignKeyAndOnlyRequiredIndexWithoutCascadeOrDrop() throws Exception {
        String ddl = normalizeSql(read(DDL));

        assertTrue(ddl.contains(
                "constraint fk_cd_ecom_stage_loadsession foreign key (loadsessionid) "
                        + "references dbo.dwh_excel_load_session(id)"
        ));
        assertFalse(ddl.contains(
                "fk_cd_ecom_stage_loadsession foreign key (loadsessionid) "
                        + "references dbo.dwh_excel_load_session(id) on delete cascade"
        ));
        assertTrue(ddl.contains(
                "create index ix_cd_ecom_stage_loadsessionid "
                        + "on dbo.cd_ecom_stage(loadsessionid)"
        ));
        assertFalse(ddl.contains("drop table dbo.cd_ecom_stage"));
    }

    @Test
    void applicationUserHasStageDmlWithoutDdlPermissions() throws Exception {
        String permissions = normalizeSql(read(USERS_DDL));

        assertTrue(permissions.contains(
                "grant select, insert, update, delete "
                        + "on object::dbo.cd_ecom_stage to repl_service"
        ));
        assertFalse(permissions.contains(
                "grant alter on object::dbo.cd_ecom_stage to repl_service"
        ));
        assertFalse(permissions.contains(
                "grant control on object::dbo.cd_ecom_stage to repl_service"
        ));
    }

    @Test
    void requiredDeleteFieldsMigrationIsGuardedIdempotentAndNonDestructive() throws Exception {
        String migration = normalizeSql(read(REQUIRED_FIELDS_MIGRATION));

        assertTrue(migration.contains("from dbo.cd_ecom where year is null"));
        assertTrue(migration.contains("from dbo.cd_ecom_stage where year is null"));
        assertTrue(migration.contains("name is null"));
        assertTrue(migration.contains("nchar(160)"));
        assertTrue(migration.contains("nchar(8239)"));
        assertTrue(migration.contains("throw 50001"));
        assertTrue(migration.contains("legacy target or stage rows violate the required-field contract"));

        for (String table : List.of("dbo.cd_ecom", "dbo.cd_ecom_stage")) {
            assertTrue(migration.contains(
                    "alter table " + table + " alter column name nvarchar(255) not null"
            ));
            assertTrue(migration.contains(
                    "alter table " + table + " alter column year int not null"
            ));
            assertTrue(migration.contains(
                    "alter table " + table + " alter column season int not null"
            ));
            assertTrue(migration.contains(
                    "alter table " + table + " alter column day int not null"
            ));
        }

        assertEquals(8, occurrences(migration, "alter table dbo.cd_ecom"));
        assertEquals(8, occurrences(migration, "is_nullable = 1"));
        assertFalse(migration.contains("drop table"));
        assertFalse(migration.contains("delete from"));
        assertFalse(migration.contains("update "));
        assertFalse(migration.contains("truncate"));
        assertFalse(migration.contains("default"));
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
