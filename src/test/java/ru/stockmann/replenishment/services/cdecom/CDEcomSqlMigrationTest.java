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
        assertTrue(sql.contains("FOREIGN KEY (LoadSessionId) REFERENCES dbo.DWH_Excel_Load_Session(Id)"));
        assertTrue(sql.contains("dbo.CD_ecom_load_session"));
        assertTrue(sql.contains("dbo.CD_ecom_load_error"));
    }
}
