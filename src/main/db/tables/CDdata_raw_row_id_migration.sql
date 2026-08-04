IF COL_LENGTH(N'dbo.CD_data', N'RawRowId') IS NULL
BEGIN
    ALTER TABLE dbo.CD_data
        ADD RawRowId BIGINT NULL;
END;
GO

IF COL_LENGTH(N'dbo.CD_data_stage', N'RawRowId') IS NULL
BEGIN
    ALTER TABLE dbo.CD_data_stage
        ADD RawRowId BIGINT NULL;
END;
GO
