/* ============================================================
   1. SERVER LOGINS
   Выполнять в master
   ============================================================ */

USE master;
GO

/* Java service user */
IF NOT EXISTS (
        SELECT 1
        FROM sys.server_principals
        WHERE name = N'Repl_Service'
    )
    BEGIN
        CREATE LOGIN [Repl_Service]
            WITH PASSWORD = 'kD0rNQsPDAq3qHjus2PkB061INnuF7iaZPhvy3p/xUX3vCkb';
    END;
GO

/* Project-wide read-only user */
IF NOT EXISTS (
        SELECT 1
        FROM sys.server_principals
        WHERE name = N'ReplenishmentREAD'
    )
    BEGIN
        CREATE LOGIN [ReplenishmentREAD]
            WITH PASSWORD = '9W_G94wLpU';
    END;
GO

/* Remove the server-level privilege left by the former service account setup. */
REVOKE ADMINISTER BULK OPERATIONS TO [ReplenishmentREAD];
GO

/* Additional read-only user */
IF NOT EXISTS (
        SELECT 1
        FROM sys.server_principals
        WHERE name = N'repl'
    )
    BEGIN
        CREATE LOGIN [repl]
            WITH PASSWORD = '333', CHECK_POLICY = OFF;
    END;
GO

/* ============================================================
   2. DATABASE USERS
   Выполнять в рабочей базе
   ============================================================ */

USE [ReplenishmentDWH];
GO

IF NOT EXISTS (
        SELECT 1
        FROM sys.database_principals
        WHERE name = N'Repl_Service'
    )
    BEGIN
        CREATE USER [Repl_Service]
            FOR LOGIN [Repl_Service];
    END;
GO

IF NOT EXISTS (
        SELECT 1
        FROM sys.database_principals
        WHERE name = N'ReplenishmentREAD'
    )
    BEGIN
        CREATE USER [ReplenishmentREAD]
            FOR LOGIN [ReplenishmentREAD];
    END;
GO

IF NOT EXISTS (
        SELECT 1
        FROM sys.database_principals
        WHERE name = N'repl'
    )
    BEGIN
        CREATE USER [repl]
            FOR LOGIN [repl];
    END;
GO


/* ============================================================
   3. RIGHTS FOR JAVA SERVICE USER: Repl_Service
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

IF OBJECT_ID(N'dbo.BulkLoadErrors', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.BulkLoadErrors TO [Repl_Service];
GO

IF OBJECT_ID(N'dbo.LoadStoreTurnoverFromCSV', N'P') IS NOT NULL
    GRANT EXECUTE ON OBJECT::dbo.LoadStoreTurnoverFromCSV TO [Repl_Service];
GO

GRANT ADMINISTER BULK OPERATIONS TO [Repl_Service];
GO

GRANT SHOWPLAN TO [Repl_Service];
GO

/* ============================================================
   4. RIGHTS FOR PROJECT-WIDE READ-ONLY USER: ReplenishmentREAD
   ============================================================ */

/*
   ReplenishmentREAD used to be the Java service account. Remove all
   object-level permissions granted by that setup before assigning its
   read-only permissions.
*/
IF OBJECT_ID(N'dbo.DWH_Excel_Load_Session') IS NOT NULL
    REVOKE SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.DWH_Excel_Load_Session FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.DWH_Excel_Load_Error') IS NOT NULL
    REVOKE SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.DWH_Excel_Load_Error FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.Weekly_data') IS NOT NULL
    REVOKE SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.Weekly_data FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.Weekly_data_raw') IS NOT NULL
    REVOKE SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.Weekly_data_raw FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.Weekly_data_stage') IS NOT NULL
    REVOKE SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.Weekly_data_stage FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.usp_WeeklyData_ProcessLoadSession') IS NOT NULL
    REVOKE EXECUTE ON OBJECT::dbo.usp_WeeklyData_ProcessLoadSession FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.CD_data') IS NOT NULL
    REVOKE SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.CD_data FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.CD_data_raw') IS NOT NULL
    REVOKE SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.CD_data_raw FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.CD_data_stage') IS NOT NULL
    REVOKE SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.CD_data_stage FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.usp_CDData_ProcessLoadSession') IS NOT NULL
    REVOKE EXECUTE ON OBJECT::dbo.usp_CDData_ProcessLoadSession FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.CD_ecom') IS NOT NULL
    REVOKE SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.CD_ecom FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.CD_ecom_raw') IS NOT NULL
    REVOKE SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.CD_ecom_raw FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.CD_ecom_stage') IS NOT NULL
    REVOKE SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.CD_ecom_stage FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.SalesByChannel_raw') IS NOT NULL
    REVOKE SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.SalesByChannel_raw FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.SalesByChannel_stage') IS NOT NULL
    REVOKE SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.SalesByChannel_stage FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.SalesByChannel') IS NOT NULL
    REVOKE SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.SalesByChannel FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.ABCData') IS NOT NULL
    REVOKE SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.ABCData FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.ABCData_STG') IS NOT NULL
    REVOKE SELECT, INSERT, UPDATE, DELETE, ALTER ON OBJECT::dbo.ABCData_STG FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.usp_ABCData_Merge') IS NOT NULL
    REVOKE EXECUTE ON OBJECT::dbo.usp_ABCData_Merge FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.StoreTurnover') IS NOT NULL
    REVOKE SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.StoreTurnover FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.BulkLoadErrors') IS NOT NULL
    REVOKE SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.BulkLoadErrors FROM [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.LoadStoreTurnoverFromCSV') IS NOT NULL
    REVOKE EXECUTE ON OBJECT::dbo.LoadStoreTurnoverFromCSV FROM [ReplenishmentREAD];
GO

/* SELECT covers all current and future project tables in dbo. */
GRANT SELECT ON SCHEMA::dbo TO [ReplenishmentREAD];
GO

GRANT SHOWPLAN TO [ReplenishmentREAD];
GO

/* ============================================================
   5. RIGHTS FOR TARGET TABLE READER: repl
   ============================================================ */

/* Remove access that is outside the target tables of ready services. */
IF OBJECT_ID(N'dbo.ABCData', N'U') IS NOT NULL
    REVOKE SELECT ON OBJECT::dbo.ABCData FROM [repl];
GO

IF OBJECT_ID(N'dbo.StoreTurnover', N'U') IS NOT NULL
    REVOKE SELECT ON OBJECT::dbo.StoreTurnover FROM [repl];
GO

IF OBJECT_ID(N'dbo.DWH_Excel_Load_Session', N'U') IS NOT NULL
    REVOKE SELECT ON OBJECT::dbo.DWH_Excel_Load_Session FROM [repl];
GO

IF OBJECT_ID(N'dbo.DWH_Excel_Load_Error', N'U') IS NOT NULL
    REVOKE SELECT ON OBJECT::dbo.DWH_Excel_Load_Error FROM [repl];
GO

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

/* ============================================================
   6. REMOVE RETIRED USER: replenishment_reader
   ============================================================ */

USE [ReplenishmentDWH];
GO

DROP USER IF EXISTS [replenishment_reader];
GO

USE master;
GO

DROP LOGIN IF EXISTS [replenishment_reader];
GO
