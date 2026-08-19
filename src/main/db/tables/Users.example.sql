/* ============================================================
   Replenishment project users and permissions

   This script is intentionally destructive ONLY for the three
   project-specific principals listed below. It can be rerun after
   database structure changes to rebuild their users and permissions
   from a clean state.

   Project principals:
     - Repl_Service      Java service account
     - ReplenishmentREAD legacy/project-wide read-only account
     - repl              target-table read-only account

   IMPORTANT:
     - Run with an administrative account, not with one of the logins
       that this script drops.
     - The script does not touch any other server logins or DB users.
     - ReplenishmentREAD is kept temporarily for backward compatibility.
   ============================================================ */

/* ============================================================
   1. RESET DATABASE USERS AND ALL OF THEIR DATABASE PERMISSIONS
   ============================================================ */

USE [ReplenishmentDWH];
GO

IF USER_ID(N'Repl_Service') IS NOT NULL
    DROP USER [Repl_Service];
GO

IF USER_ID(N'ReplenishmentREAD') IS NOT NULL
    DROP USER [ReplenishmentREAD];
GO

IF USER_ID(N'repl') IS NOT NULL
    DROP USER [repl];
GO

/* ============================================================
   2. RESET SERVER LOGINS
   ============================================================ */

USE master;
GO

IF EXISTS (
        SELECT 1
        FROM sys.server_principals
        WHERE name = N'Repl_Service'
    )
    DROP LOGIN [Repl_Service];
GO

IF EXISTS (
        SELECT 1
        FROM sys.server_principals
        WHERE name = N'ReplenishmentREAD'
    )
    DROP LOGIN [ReplenishmentREAD];
GO

IF EXISTS (
        SELECT 1
        FROM sys.server_principals
        WHERE name = N'repl'
    )
    DROP LOGIN [repl];
GO

/* ============================================================
   3. SERVER LOGINS - ЗАПУСКАТЬ ИЗ ФАЙЛА Users.sql
   Password placeholders must be replaced before execution.
   ============================================================ */

/* Java service user */
CREATE LOGIN [Repl_Service]
    WITH PASSWORD = '<REPL_SERVICE_PASSWORD>';
GO

/* Project-wide read-only user.
   Kept temporarily for backward compatibility. */
CREATE LOGIN [ReplenishmentREAD]
    WITH PASSWORD = '<REPLENISHMENT_READ_PASSWORD>';
GO

/* Additional read-only user */
CREATE LOGIN [repl]
    WITH PASSWORD = '<REPL_PASSWORD>', CHECK_POLICY = OFF;
GO

/* ============================================================
   4. DATABASE USERS
   ============================================================ */

USE [ReplenishmentDWH];
GO

CREATE USER [Repl_Service]
    FOR LOGIN [Repl_Service];
GO

CREATE USER [ReplenishmentREAD]
    FOR LOGIN [ReplenishmentREAD];
GO

CREATE USER [repl]
    FOR LOGIN [repl];
GO

/* ============================================================
   5. RIGHTS FOR JAVA SERVICE USER: Repl_Service
   ============================================================ */

GRANT SELECT, INSERT, UPDATE, DELETE
    ON OBJECT::dbo.DWH_Excel_Load_Session
    TO [Repl_Service];
GO

GRANT SELECT, INSERT, UPDATE, DELETE
    ON OBJECT::dbo.DWH_Excel_Load_Error
    TO [Repl_Service];
GO

GRANT SELECT, INSERT, UPDATE, DELETE
    ON OBJECT::dbo.Weekly_data
    TO [Repl_Service];
GO

GRANT SELECT, INSERT, UPDATE, DELETE
    ON OBJECT::dbo.Weekly_data_raw
    TO [Repl_Service];
GO

GRANT SELECT, INSERT, UPDATE, DELETE
    ON OBJECT::dbo.Weekly_data_stage
    TO [Repl_Service];
GO

GRANT EXECUTE
    ON OBJECT::dbo.usp_WeeklyData_ProcessLoadSession
    TO [Repl_Service];
GO

IF OBJECT_ID(N'dbo.CD_data', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.CD_data TO [Repl_Service];
GO

IF OBJECT_ID(N'dbo.CD_data_raw', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.CD_data_raw TO [Repl_Service];
GO

IF OBJECT_ID(N'dbo.CD_data_stage', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.CD_data_stage TO [Repl_Service];
GO

IF OBJECT_ID(N'dbo.usp_CDData_ProcessLoadSession', N'P') IS NOT NULL
    GRANT EXECUTE ON OBJECT::dbo.usp_CDData_ProcessLoadSession TO [Repl_Service];
GO

IF OBJECT_ID(N'dbo.CD_ecom', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.CD_ecom TO [Repl_Service];
GO

IF OBJECT_ID(N'dbo.CD_ecom_raw', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.CD_ecom_raw TO [Repl_Service];
GO

IF OBJECT_ID(N'dbo.CD_ecom_stage', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.CD_ecom_stage TO [Repl_Service];
GO

IF OBJECT_ID(N'dbo.SalesByChannel_raw', N'U') IS NOT NULL
    GRANT SELECT, INSERT ON OBJECT::dbo.SalesByChannel_raw TO [Repl_Service];
GO

IF OBJECT_ID(N'dbo.SalesByChannel_stage', N'U') IS NOT NULL
    GRANT SELECT, INSERT, DELETE ON OBJECT::dbo.SalesByChannel_stage TO [Repl_Service];
GO

IF OBJECT_ID(N'dbo.SalesByChannel', N'U') IS NOT NULL
    GRANT SELECT, INSERT, DELETE ON OBJECT::dbo.SalesByChannel TO [Repl_Service];
GO

IF OBJECT_ID(N'dbo.ABCData', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.ABCData TO [Repl_Service];
GO

IF OBJECT_ID(N'dbo.ABCData_STG', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.ABCData_STG TO [Repl_Service];
GO

IF OBJECT_ID(N'dbo.ABCData_STG', N'U') IS NOT NULL
    GRANT ALTER ON OBJECT::dbo.ABCData_STG TO [Repl_Service];
GO

IF OBJECT_ID(N'dbo.usp_ABCData_Merge', N'P') IS NOT NULL
    GRANT EXECUTE ON OBJECT::dbo.usp_ABCData_Merge TO [Repl_Service];
GO

IF OBJECT_ID(N'dbo.StoreTurnover', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.StoreTurnover TO [Repl_Service];
GO

IF OBJECT_ID(N'dbo.StoreTurnover_raw', N'U') IS NOT NULL
    GRANT SELECT, INSERT ON OBJECT::dbo.StoreTurnover_raw TO [Repl_Service];
GO

IF OBJECT_ID(N'dbo.StoreTurnover_stage', N'U') IS NOT NULL
    GRANT SELECT, INSERT, DELETE ON OBJECT::dbo.StoreTurnover_stage TO [Repl_Service];
GO

IF OBJECT_ID(N'dbo.BulkLoadErrors', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.BulkLoadErrors TO [Repl_Service];
GO

IF OBJECT_ID(N'dbo.LoadStoreTurnoverFromCSV', N'P') IS NOT NULL
    GRANT EXECUTE ON OBJECT::dbo.LoadStoreTurnoverFromCSV TO [Repl_Service];
GO

USE master;
GRANT ADMINISTER BULK OPERATIONS TO [Repl_Service];
GO

USE [ReplenishmentDWH];
GRANT SHOWPLAN TO [Repl_Service];
GO

/* ============================================================
   6. RIGHTS FOR PROJECT-WIDE READ-ONLY USER: ReplenishmentREAD
   ============================================================ */

/* SELECT covers all current and future project tables in dbo. */
GRANT SELECT ON SCHEMA::dbo TO [ReplenishmentREAD];
GO

GRANT SHOWPLAN TO [ReplenishmentREAD];
GO

/* ============================================================
   7. RIGHTS FOR TARGET TABLE READER: repl
   ============================================================ */

GRANT SELECT ON OBJECT::dbo.Weekly_data TO [repl];
GO

IF OBJECT_ID(N'dbo.CD_data', N'U') IS NOT NULL
    GRANT SELECT ON OBJECT::dbo.CD_data TO [repl];
GO

IF OBJECT_ID(N'dbo.CD_ecom', N'U') IS NOT NULL
    GRANT SELECT ON OBJECT::dbo.CD_ecom TO [repl];
GO

IF OBJECT_ID(N'dbo.SalesByChannel', N'U') IS NOT NULL
    GRANT SELECT ON OBJECT::dbo.SalesByChannel TO [repl];
GO
