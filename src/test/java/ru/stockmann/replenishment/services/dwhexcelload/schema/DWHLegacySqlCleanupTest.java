package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.normalizeSql;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.read;

class DWHLegacySqlCleanupTest {

    private static final Set<String> LEGACY_CDECOM_OBJECTS = Set.of(
            "cd_ecom_load_session",
            "cd_ecom_load_error",
            "usp_cdecom_processloadsession"
    );

    private static final Set<String> JAVA_PROCESSED_REFERENCE_PROCEDURES = Set.of(
            "usp_weeklydata_processloadsession",
            "usp_cddata_processloadsession",
            "usp_cdecom_processloadsession"
    );

    @Test
    void cdecomCurrentDdlDoesNotCreateLegacyObjects() throws Exception {
        String ddl = normalizeSql(read("src/main/db/tables/CDecom_ddl.sql"));

        for (String object : LEGACY_CDECOM_OBJECTS) {
            assertFalse(ddl.contains("create table dbo." + object), object);
            assertFalse(ddl.contains("create procedure dbo." + object), object);
        }

        assertFalse(ddl.contains("drop table"));
        assertFalse(ddl.contains("delete"));
        assertFalse(ddl.contains("truncate"));
        assertFalse(ddl.contains("alter table"));
        assertTrue(ddl.contains("create table dbo.cd_ecom_raw"));
        assertTrue(ddl.contains("create table dbo.cd_ecom"));
        assertTrue(ddl.contains("references dbo.dwh_excel_load_session(id)"));
    }

    @Test
    void cdecomMigrationDoesNotReferenceLegacyTables() throws Exception {
        String migration = normalizeSql(read("src/main/db/tables/CDecom_dwh_excel_migration.sql"));

        assertFalse(migration.contains("cd_ecom_load_session"));
        assertFalse(migration.contains("cd_ecom_load_error"));
        assertTrue(migration.contains("alter table dbo.cd_ecom_raw"));
        assertTrue(migration.contains("references dbo.dwh_excel_load_session(id)"));
    }

    @Test
    void usersScriptKeepsWeeklyAndCdDataProcedureGrantsButRemovesCdecomProcedureGrant() throws Exception {
        String users = normalizeSql(read("src/main/db/tables/Users.sql"));

        assertTrue(users.contains("grant execute on object::dbo.usp_weeklydata_processloadsession"));
        assertTrue(users.contains("grant execute on object::dbo.usp_cddata_processloadsession"));
        assertFalse(users.contains("grant execute on object::dbo.usp_cdecom_processloadsession"));

        assertTrue(users.contains("grant execute on object::dbo.usp_abcdata_merge"),
                "unrelated active procedure grants should remain");
        assertTrue(users.contains("grant execute on object::dbo.loadstoreturnoverfromcsv"),
                "unrelated active procedure grants should remain");
    }

    @Test
    void cleanupMigrationIsNotNeededForCdecomLegacyTables() {
        assertFalse(Files.exists(Path.of("src/main/db/tables/DWH_excel_legacy_cleanup_post_verification.sql")));
    }

    @Test
    void javaRuntimeDoesNotReferenceLegacyDwhExcelProceduresOrLegacyCdecomTables() throws Exception {
        String javaSources = allFilesUnder("src/main/java", ".java");

        for (String procedure : JAVA_PROCESSED_REFERENCE_PROCEDURES) {
            assertFalse(javaSources.contains(procedure), procedure);
        }
        assertFalse(javaSources.contains("cd_ecom_load_error"));
        assertFalse(javaSources.contains("cd_ecom_load_session"));
        assertTrue(javaSources.contains("processprocedurename"));
    }

    @Test
    void historicalSqlReferenceFilesRemainAvailable() {
        assertTrue(Files.exists(Path.of("src/main/db/procedures/usp_WeeklyData_ProcessLoadSession.sql")));
        assertTrue(Files.exists(Path.of("src/main/db/procedures/usp_CDData_ProcessLoadSession.sql")));
        assertTrue(Files.exists(Path.of("src/main/db/procedures/usp_CDEcom_ProcessLoadSession.sql")));
    }

    private static String allFilesUnder(String directory, String suffix) throws IOException {
        try (var paths = Files.walk(Path.of(directory))) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(suffix))
                    .sorted(Comparator.comparing(Path::toString))
                    .map(path -> {
                        try {
                            return Files.readString(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .reduce("", (left, right) -> left + "\n" + right)
                    .toLowerCase(Locale.ROOT);
        }
    }
}
