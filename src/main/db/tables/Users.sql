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
        WHERE name = N'ReplenishmentREAD'
    )
    BEGIN
        CREATE LOGIN [ReplenishmentREAD]
            WITH PASSWORD = '9W_G94wLpU';
    END;
GO

/* Business read-only user */
IF NOT EXISTS (
        SELECT 1
        FROM sys.server_principals
        WHERE name = N'replenishment_reader'
    )
    BEGIN
        CREATE LOGIN [replenishment_reader]
            WITH PASSWORD = 'replUser2026';
    END;
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

USE ReplenishmentDWH;
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
        WHERE name = N'replenishment_reader'
    )
    BEGIN
        CREATE USER [replenishment_reader]
            FOR LOGIN [replenishment_reader];
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
   3. RIGHTS FOR JAVA SERVICE USER: ReplenishmentREAD
   ============================================================ */

GRANT SELECT, INSERT, UPDATE, DELETE
    ON OBJECT::dbo.DWH_Excel_Load_Session
    TO [ReplenishmentREAD];
GO

GRANT SELECT, INSERT, UPDATE, DELETE
    ON OBJECT::dbo.DWH_Excel_Load_Error
    TO [ReplenishmentREAD];
GO

GRANT SELECT, INSERT, UPDATE, DELETE
    ON OBJECT::dbo.Weekly_data
    TO [ReplenishmentREAD];
GO

GRANT SELECT, INSERT, UPDATE, DELETE
    ON OBJECT::dbo.Weekly_data_raw
    TO [ReplenishmentREAD];
GO

GRANT SELECT, INSERT, UPDATE, DELETE
    ON OBJECT::dbo.Weekly_data_stage
    TO [ReplenishmentREAD];
GO

GRANT EXECUTE
    ON OBJECT::dbo.usp_WeeklyData_ProcessLoadSession
    TO [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.CD_data', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.CD_data TO [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.CD_data_raw', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.CD_data_raw TO [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.CD_data_stage', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.CD_data_stage TO [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.usp_CDData_ProcessLoadSession', N'P') IS NOT NULL
    GRANT EXECUTE ON OBJECT::dbo.usp_CDData_ProcessLoadSession TO [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.CD_ecom', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.CD_ecom TO [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.CD_ecom_raw', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.CD_ecom_raw TO [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.CD_ecom_stage', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.CD_ecom_stage TO [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.SalesByChannel_raw', N'U') IS NOT NULL
    GRANT SELECT, INSERT ON OBJECT::dbo.SalesByChannel_raw TO [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.SalesByChannel_stage', N'U') IS NOT NULL
    GRANT SELECT, INSERT, DELETE ON OBJECT::dbo.SalesByChannel_stage TO [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.SalesByChannel', N'U') IS NOT NULL
    GRANT SELECT, INSERT, DELETE ON OBJECT::dbo.SalesByChannel TO [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.ABCData', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.ABCData TO [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.ABCData_STG', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.ABCData_STG TO [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.ABCData_STG', N'U') IS NOT NULL
    GRANT ALTER ON OBJECT::dbo.ABCData_STG TO [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.usp_ABCData_Merge', N'P') IS NOT NULL
    GRANT EXECUTE ON OBJECT::dbo.usp_ABCData_Merge TO [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.StoreTurnover', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.StoreTurnover TO [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.BulkLoadErrors', N'U') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.BulkLoadErrors TO [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.LoadStoreTurnoverFromCSV', N'P') IS NOT NULL
    GRANT EXECUTE ON OBJECT::dbo.LoadStoreTurnoverFromCSV TO [ReplenishmentREAD];
GO

GRANT ADMINISTER BULK OPERATIONS TO [ReplenishmentREAD];
GO


/* ============================================================
   4. RIGHTS FOR BUSINESS READ-ONLY USER: replenishment_reader
   ============================================================ */

GRANT SELECT ON OBJECT::dbo.Weekly_data TO [replenishment_reader];
GO

IF OBJECT_ID(N'dbo.CD_data', N'U') IS NOT NULL
    GRANT SELECT ON OBJECT::dbo.CD_data TO [replenishment_reader];
GO

IF OBJECT_ID(N'dbo.CD_ecom', N'U') IS NOT NULL
    GRANT SELECT ON OBJECT::dbo.CD_ecom TO [replenishment_reader];
GO

IF OBJECT_ID(N'dbo.ABCData', N'U') IS NOT NULL
    GRANT SELECT ON OBJECT::dbo.ABCData TO [replenishment_reader];
GO

IF OBJECT_ID(N'dbo.StoreTurnover', N'U') IS NOT NULL
    GRANT SELECT ON OBJECT::dbo.StoreTurnover TO [replenishment_reader];
GO

GRANT SELECT ON OBJECT::dbo.DWH_Excel_Load_Session TO [replenishment_reader];
GO

GRANT SELECT ON OBJECT::dbo.DWH_Excel_Load_Error TO [replenishment_reader];
GO


/* ============================================================
   5. RIGHTS FOR ADDITIONAL READ-ONLY USER: repl
   ============================================================ */

GRANT SELECT ON OBJECT::dbo.Weekly_data TO [repl];
GO

IF OBJECT_ID(N'dbo.CD_data', N'U') IS NOT NULL
    GRANT SELECT ON OBJECT::dbo.CD_data TO [repl];
GO

IF OBJECT_ID(N'dbo.CD_ecom', N'U') IS NOT NULL
    GRANT SELECT ON OBJECT::dbo.CD_ecom TO [repl];
GO

IF OBJECT_ID(N'dbo.ABCData', N'U') IS NOT NULL
    GRANT SELECT ON OBJECT::dbo.ABCData TO [repl];
GO

IF OBJECT_ID(N'dbo.StoreTurnover', N'U') IS NOT NULL
    GRANT SELECT ON OBJECT::dbo.StoreTurnover TO [repl];
GO

GRANT SELECT ON OBJECT::dbo.DWH_Excel_Load_Session TO [repl];
GO

GRANT SELECT ON OBJECT::dbo.DWH_Excel_Load_Error TO [repl];
GO


/* ============================================================
   6. EXECUTE RIGHTS FOR JAVA SERVICE USER: ReplenishmentREAD
   ============================================================ */

USE ReplenishmentDWH;
GO

GRANT EXECUTE ON OBJECT::dbo.usp_WeeklyData_ProcessLoadSession
    TO [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.usp_CDData_ProcessLoadSession', N'P') IS NOT NULL
    GRANT EXECUTE ON OBJECT::dbo.usp_CDData_ProcessLoadSession
        TO [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.usp_ABCData_Merge', N'P') IS NOT NULL
    GRANT EXECUTE ON OBJECT::dbo.usp_ABCData_Merge
        TO [ReplenishmentREAD];
GO

IF OBJECT_ID(N'dbo.LoadStoreTurnoverFromCSV', N'P') IS NOT NULL
    GRANT EXECUTE ON OBJECT::dbo.LoadStoreTurnoverFromCSV
        TO [ReplenishmentREAD];
GO
