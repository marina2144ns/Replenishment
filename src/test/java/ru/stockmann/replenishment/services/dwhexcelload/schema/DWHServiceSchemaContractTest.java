package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.cdecom.process.CDEcomRawRow;
import ru.stockmann.replenishment.services.cdecom.process.CDEcomTargetRow;
import ru.stockmann.replenishment.services.cddata.process.CDDataRawRow;
import ru.stockmann.replenishment.services.cddata.process.CDDataTargetRow;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDEcomExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDDataExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.WeeklyDataExcelLoadDefinition;
import ru.stockmann.replenishment.services.weeklydata.process.WeeklyDataRawRow;
import ru.stockmann.replenishment.services.weeklydata.process.WeeklyDataTargetRow;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.businessColumns;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.definitionColumns;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.insertColumns;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.names;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.normalizeSql;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.placeholderCount;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.read;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.recordComponents;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.selectColumns;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.tableColumns;

class DWHServiceSchemaContractTest {

    private static final String WEEKLY_DDL = "src/main/db/tables/Weekly_data_ddl.sql";
    private static final String CD_DATA_DDL = "src/main/db/tables/CDdata_ddl.sql";
    private static final String CD_ECOM_DDL = "src/main/db/tables/CDecom_ddl.sql";

    private static final String WEEKLY_RAW_REPOSITORY =
            "src/main/java/ru/stockmann/replenishment/services/weeklydata/process/WeeklyDataRawRepository.java";
    private static final String WEEKLY_TARGET_REPOSITORY =
            "src/main/java/ru/stockmann/replenishment/services/weeklydata/process/WeeklyDataTargetRepository.java";
    private static final String CD_DATA_RAW_REPOSITORY =
            "src/main/java/ru/stockmann/replenishment/services/cddata/process/CDDataRawRepository.java";
    private static final String CD_DATA_TARGET_REPOSITORY =
            "src/main/java/ru/stockmann/replenishment/services/cddata/process/CDDataTargetRepository.java";
    private static final String CD_ECOM_RAW_REPOSITORY =
            "src/main/java/ru/stockmann/replenishment/services/cdecom/process/CDEcomRawRepository.java";
    private static final String CD_ECOM_TARGET_REPOSITORY =
            "src/main/java/ru/stockmann/replenishment/services/cdecom/process/CDEcomTargetRepository.java";

    @Test
    void weeklyDataRawContractMatchesDdlDefinitionRepositoryAndRawRow() throws Exception {
        List<String> business = businessColumns(tableColumns(WEEKLY_DDL, "dbo.Weekly_data_raw"));

        assertEquals(definitionColumns(new WeeklyDataExcelLoadDefinition()), business);
        assertEquals(25, business.size());
        assertEquals(withInfrastructure("rawid", "loadsessionid", "excelrownum", business),
                selectColumns(WEEKLY_RAW_REPOSITORY, "dbo.Weekly_data_raw"));
        assertEquals(toWeeklyRawRowComponents(business), recordComponents(WeeklyDataRawRow.class));
        assertInfrastructure(tableColumns(WEEKLY_DDL, "dbo.Weekly_data_raw"), true);
    }

    @Test
    void cdDataRawContractMatchesDdlDefinitionRepositoryAndRawRow() throws Exception {
        List<String> business = businessColumns(tableColumns(CD_DATA_DDL, "dbo.CD_data_raw"));

        assertEquals(definitionColumns(new CDDataExcelLoadDefinition()), business);
        assertEquals(37, business.size());
        assertEquals(withInfrastructure("id", "loadsessionid", "excelrownum", business),
                selectColumns(CD_DATA_RAW_REPOSITORY, "dbo.CD_data_raw"));
        assertEquals(toCamelComponents(withInfrastructure("id", "loadsessionid", "excelrownum", business)),
                recordComponents(CDDataRawRow.class));
        assertInfrastructure(tableColumns(CD_DATA_DDL, "dbo.CD_data_raw"), true);
    }

    @Test
    void cdecomRawContractMatchesDdlDefinitionRepositoryAndRawRow() throws Exception {
        List<String> business = businessColumns(tableColumns(CD_ECOM_DDL, "dbo.CD_ecom_raw"));

        assertEquals(definitionColumns(new CDEcomExcelLoadDefinition()), business);
        assertEquals(38, business.size());
        assertEquals(withInfrastructure("id", "loadsessionid", "excelrownum", business),
                selectColumns(CD_ECOM_RAW_REPOSITORY, "dbo.CD_ecom_raw"));
        assertEquals(toCamelComponents(withInfrastructure("id", "loadsessionid", "excelrownum", business)),
                recordComponents(CDEcomRawRow.class));
        assertInfrastructure(tableColumns(CD_ECOM_DDL, "dbo.CD_ecom_raw"), true);
    }

    @Test
    void weeklyDataTargetContractMatchesDdlTargetRowAndRepositoryInsert() throws Exception {
        List<String> insert = insertColumns(WEEKLY_TARGET_REPOSITORY, "dbo.Weekly_data");
        List<String> targetColumns = businessColumns(tableColumns(WEEKLY_DDL, "dbo.Weekly_data"));

        assertEquals(withLoadSession(targetColumns), insert);
        assertEquals(insert.size(), placeholderCount(WEEKLY_TARGET_REPOSITORY, "dbo.Weekly_data"));
        assertEquals(toWeeklyTargetRowComponents(insert), recordComponents(WeeklyDataTargetRow.class));
        assertFalse(insert.contains("id"));
        assertFalse(insert.contains("createdat"));
    }

    @Test
    void cdDataTargetContractMatchesDdlTargetRowAndRepositoryInsert() throws Exception {
        List<String> insert = insertColumns(CD_DATA_TARGET_REPOSITORY, "dbo.CD_data");
        List<String> targetColumns = businessColumns(tableColumns(CD_DATA_DDL, "dbo.CD_data"));

        assertEquals(withLoadSession(targetColumns), insert);
        assertEquals(insert.size(), placeholderCount(CD_DATA_TARGET_REPOSITORY, "dbo.CD_data"));
        assertEquals(toCamelComponents(insert), recordComponents(CDDataTargetRow.class));
        assertFalse(insert.contains("id"));
        assertFalse(insert.contains("createdat"));
    }

    @Test
    void cdecomTargetContractMatchesDdlTargetRowAndRepositoryInsert() throws Exception {
        List<String> insert = insertColumns(CD_ECOM_TARGET_REPOSITORY, "dbo.CD_ecom");
        List<String> targetColumns = businessColumns(tableColumns(CD_ECOM_DDL, "dbo.CD_ecom"));

        assertEquals(withLoadSession(targetColumns), insert);
        assertEquals(insert.size(), placeholderCount(CD_ECOM_TARGET_REPOSITORY, "dbo.CD_ecom"));
        assertEquals(toCamelComponents(insert), recordComponents(CDEcomTargetRow.class));
        assertFalse(insert.contains("id"));
        assertFalse(insert.contains("createdat"));
    }

    @Test
    void migrationsMatchCurrentDdlEndStateForAuditedChanges() throws Exception {
        String weeklyDdl = normalizeSql(read(WEEKLY_DDL));
        String weeklyTextMigration = normalizeSql(read("src/main/db/tables/Weekly_data_raw_text_migration.sql"));
        for (String column : definitionColumns(new WeeklyDataExcelLoadDefinition())) {
            if (weeklyTextMigration.contains(column.toLowerCase() + " nvarchar(4000)")) {
                assertTrue(weeklyDdl.contains(column.toLowerCase() + " nvarchar(4000) null"), column);
            }
        }

        String cdDataDdl = normalizeSql(read(CD_DATA_DDL));
        String cdDataTextMigration = normalizeSql(read("src/main/db/tables/CDdata_raw_text_migration.sql"));
        for (String column : definitionColumns(new CDDataExcelLoadDefinition())) {
            if (cdDataTextMigration.contains(column.toLowerCase() + " nvarchar(4000)")) {
                assertTrue(cdDataDdl.contains(column.toLowerCase() + " nvarchar(4000) null"), column);
            }
        }

        String cdecomDdl = normalizeSql(read(CD_ECOM_DDL));
        String cdecomMigration = normalizeSql(read("src/main/db/tables/CDecom_dwh_excel_migration.sql"));
        String loadSessionNotNullMigration = normalizeSql(read(
                "src/main/db/tables/CDecom_target_load_session_not_null_migration.sql"
        ));
        assertTrue(cdecomDdl.contains("excelrownum bigint null"));
        assertTrue(cdecomMigration.contains("add excelrownum bigint null"));
        assertTrue(cdecomDdl.contains("loadsessionid bigint not null"));
        assertTrue(loadSessionNotNullMigration.contains("alter column loadsessionid bigint not null"));
    }

    private static void assertInfrastructure(List<DWHSchemaTestSupport.ColumnDef> columns, boolean createdAt) {
        assertColumn(columns, "id", "bigint");
        assertTrue(columns.stream()
                .filter(c -> c.name().equals("id"))
                .findFirst()
                .orElseThrow()
                .contains("identity"));
        assertColumn(columns, "loadsessionid", "bigint");
        assertColumn(columns, "excelrownum", "bigint");
        if (createdAt) {
        assertTrue(columns.stream()
                .filter(c -> c.name().equals("createdat"))
                .findFirst()
                .orElseThrow()
                .type()
                .startsWith("datetime2"));
        }
    }

    private static void assertColumn(List<DWHSchemaTestSupport.ColumnDef> columns, String name, String type) {
        DWHSchemaTestSupport.ColumnDef column = columns.stream()
                .filter(c -> c.name().equals(name))
                .findFirst()
                .orElseThrow();
        assertEquals(type, column.type(), name);
    }

    private static List<String> withInfrastructure(
            String idName,
            String loadSessionName,
            String excelRowName,
            List<String> business
    ) {
        List<String> result = new java.util.ArrayList<>();
        result.add(idName);
        result.add(loadSessionName);
        result.add(excelRowName);
        result.addAll(business);
        return result;
    }

    private static List<String> withLoadSession(List<String> business) {
        List<String> result = new java.util.ArrayList<>();
        result.add("loadsessionid");
        result.addAll(business);
        return result;
    }

    private static List<String> toWeeklyRawRowComponents(List<String> business) {
        List<String> components = new java.util.ArrayList<>();
        components.add("loadsessionid");
        components.add("rawid");
        components.add("excelrownum");
        components.addAll(toCamelComponents(business));
        return components;
    }

    private static List<String> toWeeklyTargetRowComponents(List<String> insert) {
        return toCamelComponents(insert);
    }

    private static List<String> toCamelComponents(List<String> columns) {
        return columns.stream().map(DWHServiceSchemaContractTest::toCamel).toList();
    }

    private static String toCamel(String column) {
        String[] parts = column.split("_");
        StringBuilder result = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            result.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return result.toString().toLowerCase(java.util.Locale.ROOT);
    }
}
