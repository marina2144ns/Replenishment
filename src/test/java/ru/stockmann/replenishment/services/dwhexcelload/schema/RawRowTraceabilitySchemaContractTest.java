package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.normalizeSql;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.read;

class RawRowTraceabilitySchemaContractTest {

    @Test
    void rawPrimaryKeysAndTraceColumnsUseBigint() throws Exception {
        assertTraceContract(
                "src/main/db/tables/Weekly_data_ddl.sql",
                "dbo.weekly_data_raw",
                "dbo.weekly_data",
                "dbo.weekly_data_stage"
        );
        assertTraceContract(
                "src/main/db/tables/CDdata_ddl.sql",
                "dbo.cd_data_raw",
                "dbo.cd_data",
                "dbo.cd_data_stage"
        );
        assertTraceContract(
                "src/main/db/tables/CDecom_ddl.sql",
                "dbo.cd_ecom_raw",
                "dbo.cd_ecom",
                "dbo.cd_ecom_stage"
        );
    }

    @Test
    void migrationsAreSafeNullableAdditionsWithoutForeignKeysOrBackfill() throws Exception {
        for (Migration migration : List.of(
                new Migration("src/main/db/tables/Weekly_data_raw_row_id_migration.sql",
                        "dbo.weekly_data", "dbo.weekly_data_stage"),
                new Migration("src/main/db/tables/CDdata_raw_row_id_migration.sql",
                        "dbo.cd_data", "dbo.cd_data_stage"),
                new Migration("src/main/db/tables/CDecom_raw_row_id_migration.sql",
                        "dbo.cd_ecom", "dbo.cd_ecom_stage")
        )) {
            String sql = normalizeSql(read(migration.path()));
            for (String table : List.of(migration.target(), migration.stage())) {
                assertTrue(sql.contains("col_length(n'" + table + "', n'rawrowid') is null"));
                assertTrue(sql.contains("alter table " + table + " add rawrowid bigint null"));
            }
            assertFalse(sql.contains("foreign key"));
            assertFalse(sql.contains("update "));
        }
    }

    private static void assertTraceContract(
            String ddlPath,
            String rawTable,
            String targetTable,
            String stageTable
    ) throws Exception {
        String ddl = normalizeSql(read(ddlPath));
        assertTrue(ddl.contains("create table " + rawTable));
        assertTrue(ddl.contains("id bigint identity"));
        assertTrue(ddl.contains("create table " + targetTable));
        assertTrue(ddl.contains("create table " + stageTable));
        assertTrue(DWHSchemaTestSupport.tableColumns(ddlPath, targetTable).stream()
                .anyMatch(column -> column.name().equals("rawrowid")
                        && column.type().equals("bigint")
                        && column.contains("bigint null")));
        assertTrue(DWHSchemaTestSupport.tableColumns(ddlPath, stageTable).stream()
                .anyMatch(column -> column.name().equals("rawrowid")
                        && column.type().equals("bigint")
                        && column.contains("bigint null")));
        assertFalse(ddl.contains("foreign key (rawrowid)"));
    }

    private record Migration(String path, String target, String stage) {
    }
}
