
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
