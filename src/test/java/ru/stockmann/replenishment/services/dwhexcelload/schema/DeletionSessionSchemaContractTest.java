package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.normalizeSql;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.read;

class DeletionSessionSchemaContractTest {

    private static final String TEXT_PERIOD_MIGRATION =
            "src/main/db/tables/dwhExcelLoad_delete_text_year_month_migration.sql";
    private static final String CONSOLIDATED_MIGRATION =
            "src/main/db/tables/dwhExcelLoad_deletion_metadata_migration.sql";

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

    @Test
    void consolidatedDeletionMetadataMigrationMatchesFreshInstallContract() throws Exception {
        String migration = normalizeSql(read(CONSOLIDATED_MIGRATION));
        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("operationtype", "nvarchar(30) not null");
        columns.put("operationmode", "nvarchar(30) null");
        columns.put("deleteyear", "int null");
        columns.put("deleteweek", "int null");
        columns.put("deletemonth", "int null");
        columns.put("deleteyeartext", "nvarchar(50) null");
        columns.put("deletemonthtext", "nvarchar(50) null");
        columns.put("sourceloadsessionid", "bigint null");
        columns.put("deletecriterion", "nvarchar(50) null");
        columns.put("deleteparameter1name", "nvarchar(50) null");
        columns.put("deleteparameter1value", "nvarchar(1000) null");
        columns.put("deleteparameter2name", "nvarchar(50) null");
        columns.put("deleteparameter2value", "nvarchar(1000) null");
        columns.put("deletedrows", "bigint null");

        columns.forEach((name, definition) -> {
            assertTrue(migration.contains(
                    "if col_length(n'dbo.dwh_excel_load_session', n'" + name + "') is null"
            ), name);
            assertTrue(migration.contains("add " + name + " " + definition), name);
        });
        assertEquals(15, occurrences(migration, "col_length(n'dbo.dwh_excel_load_session'"));
        assertTrue(migration.contains(
                "constraint df_dwh_excel_load_session_operationtype default ('load') with values"
        ));
        assertTrue(migration.contains("set operationtype = n'load' where operationtype is null"));
        assertTrue(migration.contains("from sys.default_constraints dc"));
        assertTrue(migration.contains("and c.name = n'operationtype'"));
        assertFalse(migration.contains("drop column"));
        assertFalse(migration.contains("drop table"));
        assertFalse(migration.contains("truncate "));
        assertFalse(migration.contains("delete from"));
        assertFalse(migration.contains("update dbo.dwh_excel_load_session set delete"));
    }

    @Test
    void consolidatedMigrationReplacesWrongOperationTypeDefaultWithLoad() throws Exception {
        String migration = normalizeSql(read(CONSOLIDATED_MIGRATION));

        assertTrue(migration.contains("@operationtypedefaultname = dc.name"));
        assertTrue(migration.contains("@operationtypedefaultdefinition = dc.definition"));
        assertTrue(migration.contains(
                "upper(replace(@operationtypedefaultdefinition, n' ', n'')) <> n'(''load'')'"
        ));
        assertTrue(migration.contains(
                "alter table dbo.dwh_excel_load_session drop constraint"
        ));
        assertTrue(migration.contains("quotename(@operationtypedefaultname)"));
        assertTrue(migration.contains("@operationtypedefaultname is null"));
        assertTrue(migration.contains(
                "constraint df_dwh_excel_load_session_operationtype default ('load') for operationtype"
        ));
        assertFalse(migration.contains("drop column"));
        assertFalse(migration.contains("drop table"));
    }

    private static int occurrences(String value, String fragment) {
        return value.split(java.util.regex.Pattern.quote(fragment), -1).length - 1;
    }
}
