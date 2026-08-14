package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;

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
}
