package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.normalizeSql;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.read;

class DeletionSessionSchemaContractTest {

    @Test
    void migrationAddsDeletionFieldsSafelyWithoutBackfillOrProcedure() throws Exception {
        String sql = normalizeSql(read(
                "src/main/db/tables/dwhExcelLoad_delete_session_migration.sql"
        ));

        for (String column : new String[]{
                "operationtype", "operationmode", "deleteyear", "deleteweek",
                "sourceloadsessionid", "deletedrows", "deletecriterion",
                "deleteparameter1name", "deleteparameter1value",
                "deleteparameter2name", "deleteparameter2value"
        }) {
            assertTrue(sql.contains(
                    "col_length(n'dbo.dwh_excel_load_session', n'" + column + "') is null"
            ));
        }
        assertTrue(sql.contains("default ('load')"));
        assertFalse(sql.contains("update "));
        assertFalse(sql.contains("create procedure"));
    }

    @Test
    void normalLoadInsertReliesOnLoadOperationDefault() throws Exception {
        String loader = normalizeSql(read(
                "src/main/java/ru/stockmann/replenishment/services/dwhexcelload/core/AbstractDWHExcelLoader.java"
        ));

        assertTrue(loader.contains("insert into dbo.dwh_excel_load_session"));
        assertFalse(loader.contains("operationtype, operationmode"));
    }
}
