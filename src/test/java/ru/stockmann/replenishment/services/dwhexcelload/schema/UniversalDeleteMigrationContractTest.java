package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.normalizeSql;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.read;

class UniversalDeleteMigrationContractTest {

    @Test
    void migrationIsIdempotentAdditiveAndVerified() throws Exception {
        String migration = normalizeSql(read(
                "src/main/db/migration/2026-10-universal-delete/01_production_migration.sql"
        ));
        String verify = normalizeSql(read(
                "src/main/db/migration/2026-10-universal-delete/02_verify.sql"
        ));

        assertTrue(migration.contains(
                "if col_length(n'dbo.dwh_excel_load_session', n'deletecriteria') is null"
        ));
        assertTrue(migration.contains("add deletecriteria nvarchar(max) null"));
        assertFalse(migration.contains("drop "));
        assertFalse(migration.contains("delete from"));
        assertFalse(migration.contains("truncate "));
        assertTrue(verify.contains("name = n'deletecriteria'"));
        assertTrue(verify.contains("max_length = -1"));
        assertTrue(verify.contains("is_nullable = 1"));
    }
}
