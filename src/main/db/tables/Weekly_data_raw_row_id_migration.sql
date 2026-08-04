IF COL_LENGTH(N'dbo.Weekly_data', N'RawRowId') IS NULL
BEGIN
    ALTER TABLE dbo.Weekly_data
        ADD RawRowId BIGINT NULL;
END;
GO

IF COL_LENGTH(N'dbo.Weekly_data_stage', N'RawRowId') IS NULL
BEGIN
    ALTER TABLE dbo.Weekly_data_stage
        ADD RawRowId BIGINT NULL;
END;
GO
