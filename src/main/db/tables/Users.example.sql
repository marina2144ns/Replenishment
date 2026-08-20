/* ============================================================
   Replenishment project users and permissions

   This script rebuilds the Java service account, but MUST preserve
   externally used read-only logins:
     - Repl_Service      Java service account
     - ReplenishmentREAD 1C/project-wide read-only account
     - repl              target-table read-only account

   IMPORTANT:
     - Run with an administrative account, not with Repl_Service.
     - ReplenishmentREAD is used by 1C: NEVER DROP or reset its password
       automatically. Preserve the existing server login/SID when present.
     - repl must also be preserved when present.
     - For preserved logins, database users are repaired with ALTER USER
       ... WITH LOGIN so restores cannot leave orphaned/mismatched users.
     - Password placeholders are used only when a login does not exist and
       must be replaced with the real password before execution.
   ============================================================ */

/* ============================================================
   1. RESET ONLY JAVA SERVICE DATABASE USER
   ============================================================ */

USE [ReplenishmentDWH];
GO

IF USER_ID(N'Repl_Service') IS NOT NULL
    DROP USER [Repl_Service];
GO

/* ReplenishmentREAD and repl are intentionally NOT dropped. */

/* ============================================================
   2. RESET ONLY JAVA SERVICE SERVER LOGIN
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

/* ReplenishmentREAD and repl are intentionally NOT dropped. */

/* ============================================================
   3. SERVER LOGINS
   Password placeholders must be replaced before execution when
   the corresponding login does not already exist.
   ============================================================ */

/* Java service user */
CREATE LOGIN [Repl_Service]
    WITH PASSWORD = '<REPL_SERVICE_PASSWORD>',
         DEFAULT_DATABASE = [ReplenishmentDWH];
GO

/* 1C/project-wide read-only user: preserve existing login/password/SID. */
IF NOT EXISTS (
        SELECT 1
        FROM sys.server_principals
        WHERE name = N'ReplenishmentREAD'
    )
BEGIN
    CREATE LOGIN [ReplenishmentREAD]
        WITH PASSWORD = '<REPLENISHMENT_READ_PASSWORD>',
             DEFAULT_DATABASE = [ReplenishmentDWH];
END
ELSE
BEGIN
    ALTER LOGIN [ReplenishmentREAD] ENABLE;
    ALTER LOGIN [ReplenishmentREAD]
        WITH DEFAULT_DATABASE = [ReplenishmentDWH];
END;
GO

GRANT CONNECT SQL TO [ReplenishmentREAD];
GO

/* Additional target-table read-only user: preserve existing login/password/SID. */
IF NOT EXISTS (
        SELECT 1
        FROM sys.server_principals
        WHERE name = N'repl'
    )
BEGIN
    CREATE LOGIN [repl]
        WITH PASSWORD = '<REPL_PASSWORD>',
             CHECK_POLICY = OFF,
             DEFAULT_DATABASE = [ReplenishmentDWH];
END
ELSE
BEGIN
    ALTER LOGIN [repl] ENABLE;
    ALTER LOGIN [repl]
        WITH DEFAULT_DATABASE = [ReplenishmentDWH];
END;
GO

GRANT CONNECT SQL TO [repl];
GO

/* ============================================================
   4. DATABASE USERS
   ============================================================ */

USE [ReplenishmentDWH];
GO

CREATE USER [Repl_Service]
    FOR LOGIN [Repl_Service];
GO

/* ReplenishmentREAD: create if missing, otherwise repair login mapping. */
IF USER_ID(N'ReplenishmentREAD') IS NULL
BEGIN
    CREATE USER [ReplenishmentREAD]
        FOR LOGIN [ReplenishmentREAD];
END
ELSE
BEGIN
    ALTER USER [ReplenishmentREAD]
        WITH LOGIN = [ReplenishmentREAD];
END;
GO

GRANT CONNECT TO [ReplenishmentREAD];
GO

/* repl: create if missing, otherwise repair login mapping. */
IF USER_ID(N'repl') IS NULL
BEGIN
    CREATE USER [repl]
        FOR LOGIN [repl];
END
ELSE
BEGIN
    ALTER USER [repl]
        WITH LOGIN = [repl];
END;
GO

GRANT CONNECT TO [repl];
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
   6. RIGHTS FOR 1C/PROJECT-WIDE READ-ONLY USER: ReplenishmentREAD
   ============================================================ */

/* SELECT covers all current and future objects in dbo. */
GRANT SELECT ON SCHEMA::dbo TO [ReplenishmentREAD];
GO

GRANT SHOWPLAN TO [ReplenishmentREAD];
GO

/* ============================================================
   7. RIGHTS FOR TARGET TABLE READER: repl
   ============================================================ */

IF OBJECT_ID(N'dbo.Weekly_data', N'U') IS NOT NULL
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

IF OBJECT_ID(N'dbo.StoreTurnover', N'U') IS NOT NULL
    GRANT SELECT ON OBJECT::dbo.StoreTurnover TO [repl];
GO

IF OBJECT_ID(N'dbo.ABCData', N'U') IS NOT NULL
    GRANT SELECT ON OBJECT::dbo.ABCData TO [repl];
GO
