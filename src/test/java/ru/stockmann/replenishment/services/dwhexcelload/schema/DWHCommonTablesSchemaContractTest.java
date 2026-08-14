package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.names;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.tableColumns;

class DWHCommonTablesSchemaContractTest {

    @Test
    void loadSessionTableExposesStatusServiceColumns() throws Exception {
        List<DWHSchemaTestSupport.ColumnDef> columns = tableColumns(
                "src/main/db/tables/dwhExcelLoad_ddl.SQL",
                "dbo.DWH_Excel_Load_Session"
        );

        assertEquals(List.of(
                "id",
                "loadtypecode",
                "servicename",
                "filename",
                "filepath",
                "operationtype",
                "operationmode",
                "deleteyear",
                "deleteweek",
                "deletemonth",
                "sourceloadsessionid",
                "deletecriterion",
                "deleteparameter1name",
                "deleteparameter1value",
                "deleteparameter2name",
                "deleteparameter2value",
                "deletedrows",
                "status",
                "startedat",
                "finishedat",
                "message"
        ), names(columns));
        assertColumn(columns, "id", "bigint", "identity(1,1)", "not null");
        assertColumn(columns, "loadtypecode", "nvarchar(50)", "not null");
        assertColumn(columns, "servicename", "nvarchar(200)", "not null");
        assertColumn(columns, "filename", "nvarchar(500)", "null");
        assertColumn(columns, "filepath", "nvarchar(1000)", "null");
        assertColumn(columns, "operationtype", "nvarchar(30)", "not null", "default ('load')");
        assertColumn(columns, "operationmode", "nvarchar(30)", "null");
        assertColumn(columns, "deleteyear", "int", "null");
        assertColumn(columns, "deleteweek", "int", "null");
        assertColumn(columns, "deletemonth", "int", "null");
        assertColumn(columns, "sourceloadsessionid", "bigint", "null");
        assertColumn(columns, "deletecriterion", "nvarchar(50)", "null");
        assertColumn(columns, "deleteparameter1name", "nvarchar(50)", "null");
        assertColumn(columns, "deleteparameter1value", "nvarchar(1000)", "null");
        assertColumn(columns, "deleteparameter2name", "nvarchar(50)", "null");
        assertColumn(columns, "deleteparameter2value", "nvarchar(1000)", "null");
        assertColumn(columns, "deletedrows", "bigint", "null");
        assertColumn(columns, "status", "nvarchar(30)", "not null");
        assertColumn(columns, "startedat", "datetime2(0)", "not null");
        assertColumn(columns, "finishedat", "datetime2(0)", "null");
        assertColumn(columns, "message", "nvarchar(2000)", "null");
    }

    @Test
    void loadErrorTableMatchesServiceErrorRepositoryContract() throws Exception {
        List<DWHSchemaTestSupport.ColumnDef> columns = tableColumns(
                "src/main/db/tables/dwhExcelLoad_ddl.SQL",
                "dbo.DWH_Excel_Load_Error"
        );

        assertEquals(List.of(
                "id",
                "loadsessionid",
                "loadtypecode",
                "errorlayer",
                "excelrownum",
                "rawid",
                "fieldname",
                "errorcode",
                "errorreason",
                "errormessage",
                "createdat"
        ), names(columns));
        assertColumn(columns, "loadsessionid", "bigint", "not null");
        assertColumn(columns, "excelrownum", "bigint", "null");
        assertColumn(columns, "rawid", "bigint", "null");
        assertColumn(columns, "fieldname", "nvarchar(255)", "null");
        assertColumn(columns, "errorcode", "nvarchar(100)", "null");
        assertColumn(columns, "errorreason", "nvarchar(500)", "null");
        assertColumn(columns, "errormessage", "nvarchar(4000)", "not null");
    }

    private static void assertColumn(
            List<DWHSchemaTestSupport.ColumnDef> columns,
            String name,
            String type,
            String... fragments
    ) {
        DWHSchemaTestSupport.ColumnDef column = columns.stream()
                .filter(c -> c.name().equals(name))
                .findFirst()
                .orElseThrow();
        assertEquals(type, column.type(), name);
        for (String fragment : fragments) {
            assertTrue(column.contains(fragment), name + " should contain " + fragment);
        }
    }
}
