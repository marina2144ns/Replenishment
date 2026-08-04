IF COL_LENGTH(N'dbo.CD_ecom', N'RawRowId') IS NULL
BEGIN
    ALTER TABLE dbo.CD_ecom
        ADD RawRowId BIGINT NULL;
END;
GO

IF COL_LENGTH(N'dbo.CD_ecom_stage', N'RawRowId') IS NULL
BEGIN
    ALTER TABLE dbo.CD_ecom_stage
        ADD RawRowId BIGINT NULL;
END;
GO
