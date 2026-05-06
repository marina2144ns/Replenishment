

IF OBJECT_ID('dbo.Weekly_data', 'U') IS NOT NULL
    BEGIN
        DROP TABLE dbo.Weekly_data;
    END
GO

IF OBJECT_ID('dbo.Weekly_data_raw', 'U') IS NOT NULL
    BEGIN
        DROP TABLE dbo.Weekly_data_raw;
    END
GO
