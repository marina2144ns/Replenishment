IF OBJECT_ID(N'dbo.StoreTurnover', N'U') IS NULL
    THROW 51000, 'dbo.StoreTurnover must exist before StoreTurnover v2 migration', 1;
GO

IF COL_LENGTH(N'dbo.StoreTurnover', N'SKU') IS NULL
    THROW 51001, 'dbo.StoreTurnover.SKU is required for StoreTurnover v2 migration', 1;
GO

DECLARE @SkuType SYSNAME;
DECLARE @SkuMaxLength SMALLINT;
DECLARE @SkuNullable BIT;
SELECT @SkuType = TYPE_NAME(system_type_id),
       @SkuMaxLength = max_length,
       @SkuNullable = is_nullable
FROM sys.columns
WHERE object_id = OBJECT_ID(N'dbo.StoreTurnover') AND name = N'SKU';

IF @SkuType NOT IN (N'varchar', N'nvarchar')
    THROW 51002, 'dbo.StoreTurnover.SKU has an incompatible SQL type', 1;

IF @SkuType = N'varchar' OR @SkuMaxLength < 510 OR @SkuNullable = 0
BEGIN
    DECLARE @SkuTargetLength NVARCHAR(20) =
        CASE
            WHEN @SkuMaxLength = -1 THEN N'MAX'
            WHEN @SkuType = N'varchar' AND @SkuMaxLength > 4000 THEN N'MAX'
            WHEN @SkuType = N'nvarchar' AND @SkuMaxLength / 2 > 255
                THEN CONVERT(NVARCHAR(20), @SkuMaxLength / 2)
            WHEN @SkuType = N'varchar' AND @SkuMaxLength > 255
                THEN CONVERT(NVARCHAR(20), @SkuMaxLength)
            ELSE N'255'
        END;

    DECLARE @SkuAlterSql NVARCHAR(MAX);
    SET @SkuAlterSql =
        N'ALTER TABLE dbo.StoreTurnover ALTER COLUMN SKU NVARCHAR('
        + @SkuTargetLength
        + N') NULL';

    EXEC sys.sp_executesql @SkuAlterSql;
END;
GO

IF COL_LENGTH(N'dbo.StoreTurnover', N'StoreRus') IS NULL
    THROW 51003, 'dbo.StoreTurnover.StoreRus is required for StoreTurnover v2 migration', 1;
GO

DECLARE @StoreRusType SYSNAME;
DECLARE @StoreRusMaxLength SMALLINT;
DECLARE @StoreRusNullable BIT;
SELECT @StoreRusType = TYPE_NAME(system_type_id),
       @StoreRusMaxLength = max_length,
       @StoreRusNullable = is_nullable
FROM sys.columns
WHERE object_id = OBJECT_ID(N'dbo.StoreTurnover') AND name = N'StoreRus';

IF @StoreRusType NOT IN (N'varchar', N'nvarchar')
    THROW 51004, 'dbo.StoreTurnover.StoreRus has an incompatible SQL type', 1;

IF @StoreRusType = N'varchar' OR @StoreRusMaxLength < 510 OR @StoreRusNullable = 0
BEGIN
    DECLARE @StoreRusTargetLength NVARCHAR(20) =
        CASE
            WHEN @StoreRusMaxLength = -1 THEN N'MAX'
            WHEN @StoreRusType = N'varchar' AND @StoreRusMaxLength > 4000 THEN N'MAX'
            WHEN @StoreRusType = N'nvarchar' AND @StoreRusMaxLength / 2 > 255
                THEN CONVERT(NVARCHAR(20), @StoreRusMaxLength / 2)
            WHEN @StoreRusType = N'varchar' AND @StoreRusMaxLength > 255
                THEN CONVERT(NVARCHAR(20), @StoreRusMaxLength)
            ELSE N'255'
        END;

    DECLARE @StoreRusAlterSql NVARCHAR(MAX);
    SET @StoreRusAlterSql =
        N'ALTER TABLE dbo.StoreTurnover ALTER COLUMN StoreRus NVARCHAR('
        + @StoreRusTargetLength
        + N') NULL';

    EXEC sys.sp_executesql @StoreRusAlterSql;
END;
GO

IF OBJECT_ID(N'dbo.StoreTurnover_raw', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.StoreTurnover_raw (
        Id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        LoadSessionId BIGINT NOT NULL,
        ExcelRowNum BIGINT NULL,
        sku NVARCHAR(4000) NULL, period NVARCHAR(4000) NULL, storeRus NVARCHAR(4000) NULL,
        remainingSum NVARCHAR(4000) NULL, remainingDays NVARCHAR(4000) NULL,
        salesQuantity NVARCHAR(4000) NULL, sales NVARCHAR(4000) NULL, asp NVARCHAR(4000) NULL,
        revenue NVARCHAR(4000) NULL, gp NVARCHAR(4000) NULL, discountTotal NVARCHAR(4000) NULL,
        CreatedAt DATETIME2(0) NOT NULL CONSTRAINT DF_StoreTurnover_raw_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT FK_StoreTurnover_raw_LoadSession FOREIGN KEY (LoadSessionId)
            REFERENCES dbo.DWH_Excel_Load_Session(Id)
    );
END;
GO

IF OBJECT_ID(N'dbo.StoreTurnover_stage', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.StoreTurnover_stage (
        LoadSessionId BIGINT NOT NULL,
        ExcelRowNum BIGINT NULL,
        sku NVARCHAR(255) NOT NULL,
        period DATE NOT NULL,
        storeRus NVARCHAR(255) NOT NULL,
        remainingSum INT NOT NULL, remainingDays INT NOT NULL, salesQuantity INT NOT NULL,
        sales INT NOT NULL, asp INT NOT NULL, revenue INT NOT NULL, gp INT NOT NULL,
        discountTotal INT NOT NULL,
        RawRowId BIGINT NOT NULL,
        CONSTRAINT FK_StoreTurnover_stage_LoadSession FOREIGN KEY (LoadSessionId)
            REFERENCES dbo.DWH_Excel_Load_Session(Id),
        CONSTRAINT FK_StoreTurnover_stage_RawRow FOREIGN KEY (RawRowId)
            REFERENCES dbo.StoreTurnover_raw(Id)
    );
END;
GO

IF COL_LENGTH(N'dbo.StoreTurnover', N'LoadSessionId') IS NULL
    ALTER TABLE dbo.StoreTurnover ADD LoadSessionId BIGINT NULL;
GO

IF COL_LENGTH(N'dbo.StoreTurnover', N'RawRowId') IS NULL
    ALTER TABLE dbo.StoreTurnover ADD RawRowId BIGINT NULL;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE parent_object_id = OBJECT_ID(N'dbo.StoreTurnover')
      AND name = N'FK_StoreTurnover_LoadSession'
)
    ALTER TABLE dbo.StoreTurnover WITH CHECK
        ADD CONSTRAINT FK_StoreTurnover_LoadSession
        FOREIGN KEY (LoadSessionId) REFERENCES dbo.DWH_Excel_Load_Session(Id);
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.StoreTurnover')
      AND name = N'IX_StoreTurnover_LoadSessionId'
)
    CREATE INDEX IX_StoreTurnover_LoadSessionId ON dbo.StoreTurnover(LoadSessionId);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.StoreTurnover_raw') AND name = N'IX_StoreTurnover_raw_LoadSessionId_Id')
    CREATE INDEX IX_StoreTurnover_raw_LoadSessionId_Id ON dbo.StoreTurnover_raw(LoadSessionId, Id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.StoreTurnover_stage') AND name = N'IX_StoreTurnover_stage_LoadSessionId')
    CREATE INDEX IX_StoreTurnover_stage_LoadSessionId ON dbo.StoreTurnover_stage(LoadSessionId);
GO
