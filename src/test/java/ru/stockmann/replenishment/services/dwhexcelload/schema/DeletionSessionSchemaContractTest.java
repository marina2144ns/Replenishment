package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.normalizeSql;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.read;

class DeletionSessionSchemaContractTest {

    private static final String TEXT_PERIOD_MIGRATION =
            "src/main/db/tables/dwhExcelLoad_delete_text_year_month_migration.sql";

    @Test
    void normalLoadInsertReliesOnLoadOperationDefault() throws Exception {
        String loader = normalizeSql(read(
                "src/main/java/ru/stockmann/replenishment/services/dwhexcelload/core/AbstractDWHExcelLoader.java"
        ));

        assertTrue(loader.contains("insert into dbo.dwh_excel_load_session"));
        assertFalse(loader.contains("operationtype, operationmode"));
    }

    @Test
    void textYearMonthMigrationIsIdempotentAdditiveAndNonDestructive() throws Exception {
        String migration = normalizeSql(read(TEXT_PERIOD_MIGRATION));

        assertTrue(migration.contains(
                "if col_length('dbo.dwh_excel_load_session', 'deleteyeartext') is null"
        ));
        assertTrue(migration.contains("add deleteyeartext nvarchar(50) null"));
        assertTrue(migration.contains(
                "if col_length('dbo.dwh_excel_load_session', 'deletemonthtext') is null"
        ));
        assertTrue(migration.contains("add deletemonthtext nvarchar(50) null"));
        assertFalse(migration.contains("update "));
        assertFalse(migration.contains("delete from"));
        assertFalse(migration.contains("drop "));
        assertFalse(migration.contains("truncate "));
    }
}
