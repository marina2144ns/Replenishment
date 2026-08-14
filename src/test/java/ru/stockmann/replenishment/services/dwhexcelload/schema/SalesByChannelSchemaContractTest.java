package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String USERS_DDL = "src/main/db/tables/Users.example.sql";
    private static final String ZERO_METRICS_MIGRATION =
            "src/main/db/tables/salesbychannel_zero_metrics_migration.sql";

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

        List<String> publishedColumns = concat(BUSINESS_COLUMNS, List.of("rawrowid"), List.of());
        assertEquals(concat(List.of("loadsessionid", "excelrownum"), publishedColumns, List.of()),
                names(stage));
        assertEquals(concat(List.of("id", "loadsessionid"), publishedColumns, List.of("createdat")),
                names(target));
        assertEquals(publishedColumns, businessColumns(stage));
        assertEquals(publishedColumns, businessColumns(target));

        assertColumn(stage, "loadsessionid", "bigint", false);
        assertColumn(stage, "excelrownum", "bigint", true);
        assertColumn(stage, "rawrowid", "bigint", true);
        assertFalse(names(stage).contains("id"));
        assertFalse(names(stage).contains("createdat"));

        assertEquals("bigint", column(target, "id").type());
        assertTrue(column(target, "id").contains("identity(1,1)"));
        assertTrue(column(target, "id").contains("not null"));
        assertTrue(column(target, "id").contains("primary key"));
        assertColumn(target, "loadsessionid", "bigint", false);
        assertColumn(target, "rawrowid", "bigint", true);
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
    void zeroMetricsAreNotNullableInBothStageAndTargetWhilePeriodRemainsText() throws Exception {
        List<DWHSchemaTestSupport.ColumnDef> stage = tableColumns(DDL, STAGE);
        List<DWHSchemaTestSupport.ColumnDef> target = tableColumns(DDL, TARGET);

        for (List<DWHSchemaTestSupport.ColumnDef> columns : List.of(stage, target)) {
            assertColumn(columns, "year", "nvarchar(50)", false);
            assertColumn(columns, "month", "nvarchar(50)", false);
            assertColumn(columns, "salesquantity", "int", false);
            for (String name : List.of("salescurr", "gm", "discountttl", "turnovercurr")) {
                assertColumn(columns, name, "decimal(18,2)", false);
            }
        }
    }

    @Test
    void zeroMetricsMigrationUpdatesOnlyMetricNullsThenGuardsNotNullAlter() throws Exception {
        String migration = normalizeSql(read(ZERO_METRICS_MIGRATION));

        for (String table : List.of("dbo.salesbychannel", "dbo.salesbychannel_stage")) {
            assertTrue(migration.contains(
                    "update " + table + " set salesquantity = 0 where salesquantity is null"
            ));
            assertTrue(migration.contains(
                    "alter table " + table + " alter column salesquantity int not null"
            ));
            for (String field : List.of("salescurr", "gm", "discountttl", "turnovercurr")) {
                assertTrue(migration.contains(
                        "update " + table + " set " + field + " = 0 where " + field + " is null"
                ), table + "." + field);
                assertTrue(migration.contains(
                        "alter table " + table + " alter column " + field
                                + " decimal(18,2) not null"
                ), table + "." + field);
            }
        }

        assertEquals(10, occurrences(migration, "is_nullable = 1"));
        assertFalse(migration.contains("salesbychannel_raw"));
        assertFalse(migration.contains("dwh_excel_load_session"));
        assertFalse(migration.contains("alter column year "));
        assertFalse(migration.contains("alter column month "));
        assertFalse(migration.contains("delete "));
        assertFalse(migration.contains("drop "));
        assertFalse(migration.contains("truncate "));
        assertFalse(migration.contains("default"));
    }

    @Test
    void targetHasPeriodAndLoadSessionIndexesWithoutRawRowIndex() throws Exception {
        String ddl = normalizeSql(read(DDL));

        assertTrue(ddl.contains(
                "create index ix_salesbychannel_year_month "
                        + "on dbo.salesbychannel( year , month )"
        ));
        assertTrue(ddl.contains(
                "create index ix_salesbychannel_loadsessionid "
                        + "on dbo.salesbychannel(loadsessionid)"
        ));
        assertFalse(ddl.contains("index ix_salesbychannel_rawrowid"));
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

    @Test
    void applicationUserHasOnlyRequiredObjectPermissions() throws Exception {
        String permissions = normalizeSql(read(USERS_DDL));

        assertEquals(Set.of("select", "insert"),
                grantedPermissions(permissions, "salesbychannel_raw"));
        assertEquals(Set.of("select", "insert", "delete"),
                grantedPermissions(permissions, "salesbychannel_stage"));
        assertEquals(Set.of("select", "insert", "delete"),
                grantedPermissions(permissions, "salesbychannel"));

        assertFalse(Pattern.compile(
                "grant .*? on schema::dbo to repl_service"
        ).matcher(permissions).find());
    }

    private static Set<String> grantedPermissions(String sql, String objectName) {
        Pattern grant = Pattern.compile(
                "grant ([a-z]+(?:\\s*,\\s*[a-z]+)*) on object::dbo\\."
                        + Pattern.quote(objectName) + " to repl_service"
        );
        Matcher matcher = grant.matcher(sql);
        assertTrue(matcher.find(), "Repl_Service GRANT not found for dbo." + objectName);
        String grantedPermissions = matcher.group(1);
        assertFalse(matcher.find(), "Multiple Repl_Service GRANT statements found for dbo." + objectName);
        return java.util.Arrays.stream(grantedPermissions.split(","))
                .map(String::trim)
                .collect(java.util.stream.Collectors.toSet());
    }

    private static int occurrences(String value, String token) {
        return (value.length() - value.replace(token, "").length()) / token.length();
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
