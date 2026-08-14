package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.names;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.normalizeSql;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.insertColumns;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.placeholderCount;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.read;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.tableColumns;

class WeeklyDataStageSchemaContractTest {

    private static final String WEEKLY_DDL = "src/main/db/tables/Weekly_data_ddl.sql";
    private static final String USERS_DDL = "src/main/db/tables/Users.example.sql";
    private static final String STAGE_REPOSITORY =
            "src/main/java/ru/stockmann/replenishment/services/weeklydata/process/WeeklyDataStageRepository.java";
    private static final String STAGE_TABLE = "dbo.Weekly_data_stage";
    private static final String TARGET_TABLE = "dbo.Weekly_data";
    private static final String ZERO_METRICS_MIGRATION =
            "src/main/db/tables/weeklydata_zero_metrics_migration.sql";

    @Test
    void stageTableHasTypedWeeklyDataColumnsAndNullability() throws Exception {
        List<DWHSchemaTestSupport.ColumnDef> columns = tableColumns(WEEKLY_DDL, STAGE_TABLE);

        assertEquals(List.of(
                "loadsessionid",
                "excelrownum",
                "year21",
                "week21",
                "yearcorr",
                "weekcorr",
                "year",
                "week",
                "saleschannelbpo",
                "storerusbpo",
                "storerus",
                "mfpdivisionnew",
                "mfpdepartment",
                "skuseasonbudget",
                "typeofsales",
                "totalstockpcs",
                "totalstockddp",
                "salespcs",
                "salesrub",
                "revenue",
                "gp",
                "discounttotalrub",
                "mfpdivision",
                "season",
                "month",
                "bundle",
                "seasonality",
                "rawrowid"
        ), names(columns));

        assertColumn(columns, "loadsessionid", "bigint", false);
        assertColumn(columns, "excelrownum", "bigint", true);
        assertColumn(columns, "year21", "smallint", true);
        assertColumn(columns, "week21", "smallint", true);
        assertColumn(columns, "yearcorr", "smallint", true);
        assertColumn(columns, "weekcorr", "smallint", true);
        assertColumn(columns, "year", "smallint", false);
        assertColumn(columns, "week", "smallint", false);
        assertColumn(columns, "rawrowid", "bigint", true);

        for (String name : List.of(
                "saleschannelbpo",
                "storerusbpo",
                "storerus",
                "mfpdivisionnew",
                "mfpdepartment",
                "skuseasonbudget",
                "typeofsales",
                "mfpdivision",
                "season",
                "month",
                "bundle",
                "seasonality"
        )) {
            assertColumn(columns, name, "nvarchar(255)", true);
        }

        for (String name : List.of(
                "totalstockpcs",
                "totalstockddp",
                "salespcs",
                "salesrub",
                "revenue",
                "gp",
                "discounttotalrub"
        )) {
            assertColumn(columns, name, "decimal(18,2)", false);
        }
    }

    @Test
    void zeroMetricsAreNotNullableAndMatchTargetContract() throws Exception {
        List<DWHSchemaTestSupport.ColumnDef> stage = tableColumns(WEEKLY_DDL, STAGE_TABLE);
        List<DWHSchemaTestSupport.ColumnDef> target = tableColumns(WEEKLY_DDL, TARGET_TABLE);

        for (String name : zeroMetrics()) {
            assertColumn(stage, name, "decimal(18,2)", false);
            assertColumn(target, name, "decimal(18,2)", false);
        }
    }

    @Test
    void zeroMetricsMigrationUpdatesNullsThenGuardsNotNullAlterForTargetAndStage() throws Exception {
        String migration = normalizeSql(read(ZERO_METRICS_MIGRATION));

        for (String table : List.of("dbo.weekly_data", "dbo.weekly_data_stage")) {
            for (String column : zeroMetrics()) {
                assertTrue(migration.contains(
                        "update " + table + " set " + column + " = 0 where " + column + " is null"
                ), table + "." + column);
                assertTrue(migration.contains(
                        "alter table " + table + " alter column " + column
                                + " decimal(18,2) not null"
                ), table + "." + column);
            }
        }

        assertEquals(14, occurrences(migration, "is_nullable = 1"));
        assertFalse(migration.contains("weekly_data_raw"));
        assertFalse(migration.contains("alter column year "));
        assertFalse(migration.contains("alter column week "));
        assertFalse(migration.contains("delete from"));
        assertFalse(migration.contains("drop "));
        assertFalse(migration.contains("truncate "));
        assertFalse(migration.contains("default"));
    }

    @Test
    void stageTableHasOnlyLoadSessionAccessStructures() throws Exception {
        String ddl = normalizeSql(read(WEEKLY_DDL));

        assertTrue(ddl.contains(
                "constraint fk_weekly_data_stage_load_session foreign key (loadsessionid) "
                        + "references dbo.dwh_excel_load_session(id)"
        ));
        assertFalse(ddl.contains("fk_weekly_data_stage_load_session foreign key (loadsessionid) "
                + "references dbo.dwh_excel_load_session(id) on delete cascade"));
        assertTrue(ddl.contains(
                "create index ix_weekly_data_stage_loadsessionid "
                        + "on dbo.weekly_data_stage(loadsessionid)"
        ));
        assertFalse(names(tableColumns(WEEKLY_DDL, STAGE_TABLE)).contains("id"));
        assertFalse(ddl.contains("drop table dbo.weekly_data_stage"));
    }

    @Test
    void stageRepositoryInsertContainsEveryTableColumnInExactOrder() throws Exception {
        List<String> columns = names(tableColumns(WEEKLY_DDL, STAGE_TABLE));

        assertEquals(columns, insertColumns(STAGE_REPOSITORY, STAGE_TABLE));
        assertEquals(columns.size(), placeholderCount(STAGE_REPOSITORY, STAGE_TABLE));
    }

    @Test
    void applicationUserHasStageDmlPermissionsWithoutSchemaPrivileges() throws Exception {
        String permissions = normalizeSql(read(USERS_DDL));

        assertTrue(permissions.contains(
                "grant select, insert, update, delete "
                        + "on object::dbo.weekly_data_stage to repl_service"
        ));
        assertFalse(permissions.contains(
                "grant alter on object::dbo.weekly_data_stage to repl_service"
        ));
        assertFalse(permissions.contains(
                "grant control on object::dbo.weekly_data_stage to repl_service"
        ));
    }

    private static void assertColumn(
            List<DWHSchemaTestSupport.ColumnDef> columns,
            String name,
            String type,
            boolean nullable
    ) {
        DWHSchemaTestSupport.ColumnDef column = columns.stream()
                .filter(candidate -> candidate.name().equals(name))
                .findFirst()
                .orElseThrow();

        assertEquals(type, column.type(), name);
        if (nullable) {
            assertTrue(column.contains(type + " null"), name + " should be nullable");
            assertFalse(column.contains(type + " not null"), name + " should not be NOT NULL");
        } else {
            assertTrue(column.contains(type + " not null"), name + " should be NOT NULL");
        }
    }

    private static List<String> zeroMetrics() {
        return List.of(
                "totalstockpcs", "totalstockddp", "salespcs", "salesrub",
                "revenue", "gp", "discounttotalrub"
        );
    }

    private static int occurrences(String value, String token) {
        return (value.length() - value.replace(token, "").length()) / token.length();
    }
}
