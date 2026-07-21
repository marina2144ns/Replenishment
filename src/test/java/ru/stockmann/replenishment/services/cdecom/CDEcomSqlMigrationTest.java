package ru.stockmann.replenishment.services.cdecom;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDEcomSqlMigrationTest {

    @Test
    void procedureUsesCommonDwhSessionAndErrorTables() throws Exception {
        String sql = Files.readString(Path.of("src/main/db/procedures/usp_CDEcom_ProcessLoadSession.sql"));

        assertTrue(sql.contains("FROM dbo.DWH_Excel_Load_Session"));
        assertTrue(sql.contains("DELETE FROM dbo.DWH_Excel_Load_Error"));
        assertTrue(sql.contains("INSERT INTO dbo.DWH_Excel_Load_Error"));
        assertTrue(sql.contains("r.ExcelRowNum"));
        assertFalse(sql.contains("FROM dbo.CD_ecom_load_session"));
        assertFalse(sql.contains("dbo.CD_ecom_load_error"));
    }

    @Test
    void cdecomRawDdlContainsNullableExcelRowNumAndCommonForeignKeys() throws Exception {
        String sql = Files.readString(Path.of("src/main/db/tables/CDecom_ddl.sql"));

        assertTrue(sql.contains("ExcelRowNum                 BIGINT                NULL"));
        assertTrue(sql.contains("LoadSessionId               BIGINT                NOT NULL"));
        assertTrue(sql.contains("name                        NVARCHAR(4000)        NULL"));
        assertTrue(sql.contains("skuCollection               NVARCHAR(4000)        NULL"));
        assertTrue(sql.contains("FOREIGN KEY (LoadSessionId) REFERENCES dbo.DWH_Excel_Load_Session(Id)"));
        assertTrue(sql.contains("dbo.CD_ecom_load_session"));
        assertTrue(sql.contains("dbo.CD_ecom_load_error"));
    }

    @Test
    void cdecomMigrationExpandsRawTextColumnsForValidation() throws Exception {
        String sql = Files.readString(Path.of("src/main/db/tables/CDecom_dwh_excel_migration.sql"));

        assertTrue(sql.contains("ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN name NVARCHAR(4000) NULL"));
        assertTrue(sql.contains("ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN skuCollection NVARCHAR(4000) NULL"));
    }

    @Test
    void cdecomTargetLoadSessionMigrationFailsOnExistingNullsAndAltersToNotNull() throws Exception {
        String sql = Files.readString(Path.of("src/main/db/tables/CDecom_target_load_session_not_null_migration.sql"));

        assertTrue(sql.contains("WHERE LoadSessionId IS NULL"));
        assertTrue(sql.contains("THROW 51000"));
        assertTrue(sql.contains("ALTER TABLE dbo.CD_ecom"));
        assertTrue(sql.contains("ALTER COLUMN LoadSessionId BIGINT NOT NULL"));
        assertFalse(sql.contains("UPDATE dbo.CD_ecom"));
        assertFalse(sql.contains("DELETE FROM dbo.CD_ecom"));
    }
}
