IF COL_LENGTH(N'dbo.SalesByChannel', N'RawRowId') IS NULL
BEGIN
    ALTER TABLE dbo.SalesByChannel
        ADD RawRowId BIGINT NULL;
END;
GO

IF COL_LENGTH(N'dbo.SalesByChannel_stage', N'RawRowId') IS NULL
BEGIN
    ALTER TABLE dbo.SalesByChannel_stage
        ADD RawRowId BIGINT NULL;
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.SalesByChannel')
      AND name = N'IX_SalesByChannel_year_month'
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_SalesByChannel_year_month
        ON dbo.SalesByChannel([year], [month]);
END;
GO
