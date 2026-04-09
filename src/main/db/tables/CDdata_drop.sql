USE ReplenishmentDWH;
GO

/* =========================
   DROP TABLES (safe order)
   ========================= */

IF OBJECT_ID('dbo.CD_data_Load_error', 'U') IS NOT NULL
    BEGIN
        DROP TABLE dbo.CD_data_Load_error;
    END
GO

IF OBJECT_ID('dbo.CD_data', 'U') IS NOT NULL
    BEGIN
        DROP TABLE dbo.CD_data;
    END
GO

IF OBJECT_ID('dbo.CD_data_raw', 'U') IS NOT NULL
    BEGIN
        DROP TABLE dbo.CD_data_raw;
    END
GO

IF OBJECT_ID('dbo.CD_data_Load_session', 'U') IS NOT NULL
    BEGIN
        DROP TABLE dbo.CD_data_Load_session;
    END
GO