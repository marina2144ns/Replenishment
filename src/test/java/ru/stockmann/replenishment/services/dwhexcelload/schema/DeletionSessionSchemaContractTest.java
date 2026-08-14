package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.normalizeSql;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.read;

class DeletionSessionSchemaContractTest {

    @Test
    void normalLoadInsertReliesOnLoadOperationDefault() throws Exception {
        String loader = normalizeSql(read(
                "src/main/java/ru/stockmann/replenishment/services/dwhexcelload/core/AbstractDWHExcelLoader.java"
        ));

        assertTrue(loader.contains("insert into dbo.dwh_excel_load_session"));
        assertFalse(loader.contains("operationtype, operationmode"));
    }
}
