package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.businessColumns;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.names;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.normalizeSql;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.read;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.tableColumns;

class SalesByChannelSchemaContractTest {

    private static final String DDL = "src/main/db/tables/SalesByChannel_ddl.sql";
    private static final String RAW = "dbo.SalesByChannel_raw";
    private static final String STAGE = "dbo.SalesByChannel_stage";
    private static final String TARGET = "dbo.SalesByChannel";

    private static final List<String> BUSINESS_COLUMNS = List.of(
            "seasonyear", "season6m", "yearmonth", "yearseason", "year", "month",
            "saleschanneltype", "storerus", "typeofsales", "mfpdivision", "mfpdepartment",
            "campaignsalestype", "seasonality", "skubrandtype", "salesquantity", "salescurr",
            "gm", "discountttl", "turnovercurr", "skuseasonbudget", "storerusbpo",
            "saleschannelbpo", "mfpsubdepartment", "skutm", "mfpnode", "section",
            "merchandisesubgroup", "skuphase", "skuproductclass"
    );

    @Test
    void rawHasExactOrderedUntypedContract() throws Exception {
        List<DWHSchemaTestSupport.ColumnDef> columns = tableColumns(DDL, RAW);

        assertEquals(concat(List.of("id", "loadsessionid", "excelrownum"), BUSINESS_COLUMNS,
                List.of("createdat")), names(columns));
        assertEquals("bigint", column(columns, "id").type());
        assertTrue(column(columns, "id").contains("identity(1,1)"));
        assertTrue(column(columns, "id").contains("not null"));
        assertTrue(column(columns, "id").contains("primary key"));
        assertColumn(columns, "loadsessionid", "bigint", false);
        assertColumn(columns, "excelrownum", "bigint", true);
        for (String name : BUSINESS_COLUMNS) {
            assertColumn(columns, name, "nvarchar(4000)", true);
        }
        assertCreatedAt(columns, "df_salesbychannel_raw_createdat");
    }

    @Test
    void stageAndTargetHaveExactTypedBusinessContract() throws Exception {
        List<DWHSchemaTestSupport.ColumnDef> stage = tableColumns(DDL, STAGE);
        List<DWHSchemaTestSupport.ColumnDef> target = tableColumns(DDL, TARGET);

        assertEquals(concat(List.of("loadsessionid", "excelrownum"), BUSINESS_COLUMNS, List.of()),
                names(stage));
        assertEquals(concat(List.of("id", "loadsessionid"), BUSINESS_COLUMNS, List.of("createdat")),
                names(target));
        assertEquals(BUSINESS_COLUMNS, businessColumns(stage));
        assertEquals(BUSINESS_COLUMNS, businessColumns(target));

        assertColumn(stage, "loadsessionid", "bigint", false);
        assertColumn(stage, "excelrownum", "bigint", true);
        assertFalse(names(stage).contains("id"));
        assertFalse(names(stage).contains("createdat"));

        assertEquals("bigint", column(target, "id").type());
        assertTrue(column(target, "id").contains("identity(1,1)"));
        assertTrue(column(target, "id").contains("not null"));
        assertTrue(column(target, "id").contains("primary key"));
        assertColumn(target, "loadsessionid", "bigint", false);
        assertCreatedAt(target, "df_salesbychannel_createdat");

        for (String name : BUSINESS_COLUMNS) {
            DWHSchemaTestSupport.ColumnDef stageColumn = column(stage, name);
            DWHSchemaTestSupport.ColumnDef targetColumn = column(target, name);
            assertEquals(targetColumn.type(), stageColumn.type(), name);
            assertEquals(isNullable(targetColumn), isNullable(stageColumn), name);
        }
    }

    @Test
    void typedLengthsPrecisionScaleAndNullabilityAreExact() throws Exception {
        List<DWHSchemaTestSupport.ColumnDef> stage = tableColumns(DDL, STAGE);

        for (String name : List.of(
                "seasonyear", "season6m", "yearmonth", "yearseason", "seasonality",
                "skuseasonbudget"
        )) {
            assertColumn(stage, name, "nvarchar(50)", true);
        }
        assertColumn(stage, "year", "nvarchar(50)", false);
        assertColumn(stage, "month", "nvarchar(50)", false);

        for (String name : List.of(
                "saleschanneltype", "storerus", "typeofsales", "mfpdivision", "mfpdepartment",
                "campaignsalestype", "skubrandtype", "storerusbpo", "saleschannelbpo",
                "mfpsubdepartment", "skutm", "mfpnode", "section", "merchandisesubgroup",
                "skuphase", "skuproductclass"
        )) {
            assertColumn(stage, name, "nvarchar(100)", true);
        }

        assertColumn(stage, "salesquantity", "int", false);
        for (String name : List.of("salescurr", "gm", "discountttl", "turnovercurr")) {
            assertColumn(stage, name, "decimal(18,2)", false);
        }
    }

    @Test
    void allTablesHaveLoadSessionForeignKeysAndIndexesWithoutCascade() throws Exception {
        String ddl = normalizeSql(read(DDL));

        for (String suffix : List.of("", "_raw", "_stage")) {
            String normalizedSuffix = suffix;
            assertTrue(ddl.contains(
                    "constraint fk_salesbychannel" + normalizedSuffix
                            + "_load_session foreign key (loadsessionid) "
                            + "references dbo.dwh_excel_load_session(id)"
            ));
            assertFalse(ddl.contains(
                    "constraint fk_salesbychannel" + normalizedSuffix
                            + "_load_session foreign key (loadsessionid) "
                            + "references dbo.dwh_excel_load_session(id) on delete cascade"
            ));
            assertTrue(ddl.contains(
                    "create index ix_salesbychannel" + normalizedSuffix
                            + "_loadsessionid on dbo.salesbychannel" + normalizedSuffix
                            + "(loadsessionid)"
            ));
        }
    }

    @Test
    void ddlIsCreateOnlyAndContainsOnlyTheThreeServiceTables() throws Exception {
        String ddl = normalizeSql(read(DDL));

        assertTrue(ddl.contains("create table dbo.salesbychannel "));
        assertTrue(ddl.contains("create table dbo.salesbychannel_raw "));
        assertTrue(ddl.contains("create table dbo.salesbychannel_stage "));
        assertFalse(ddl.contains("drop table"));
        assertFalse(ddl.contains("alter table"));
        assertFalse(ddl.contains("create procedure"));
        assertFalse(ddl.contains("drop procedure"));
    }

    private static void assertCreatedAt(
            List<DWHSchemaTestSupport.ColumnDef> columns,
            String constraintName
    ) {
        DWHSchemaTestSupport.ColumnDef createdAt = column(columns, "createdat");
        assertEquals("datetime2(0)", createdAt.type());
        assertTrue(createdAt.contains("datetime2(0) not null"));
        assertTrue(createdAt.contains("constraint " + constraintName + " default sysdatetime()"));
    }

    private static DWHSchemaTestSupport.ColumnDef column(
            List<DWHSchemaTestSupport.ColumnDef> columns,
            String name
    ) {
        return columns.stream()
                .filter(candidate -> candidate.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static boolean isNullable(DWHSchemaTestSupport.ColumnDef column) {
        return column.contains(column.type() + " null")
                && !column.contains(column.type() + " not null");
    }

    private static void assertColumn(
            List<DWHSchemaTestSupport.ColumnDef> columns,
            String name,
            String type,
            boolean nullable
    ) {
        DWHSchemaTestSupport.ColumnDef column = column(columns, name);
        assertEquals(type, column.type(), name);
        if (nullable) {
            assertTrue(isNullable(column), name + " should be nullable");
        } else {
            assertTrue(column.contains(type + " not null"), name + " should be NOT NULL");
        }
    }

    private static List<String> concat(
            List<String> first,
            List<String> middle,
            List<String> last
    ) {
        return java.util.stream.Stream.of(first, middle, last).flatMap(List::stream).toList();
    }
}
