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
    private static final String USERS_DDL = "src/main/db/tables/Users.sql";
    private static final String STAGE_REPOSITORY =
            "src/main/java/ru/stockmann/replenishment/services/weeklydata/process/WeeklyDataStageRepository.java";
    private static final String STAGE_TABLE = "dbo.Weekly_data_stage";

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
            assertColumn(columns, name, "decimal(18,2)", true);
        }
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
}
