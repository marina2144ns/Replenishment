USE [ReplenishmentDWH];
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

/* ============================================================================
   1. WeeklyData zero-metric contract
   ============================================================================ */

DECLARE @WeeklyMetrics TABLE (
    TableName SYSNAME NOT NULL,
    ColumnName SYSNAME NOT NULL,
    SqlType NVARCHAR(50) NOT NULL
);

INSERT INTO @WeeklyMetrics(TableName, ColumnName, SqlType)
VALUES
(N'Weekly_data', N'TotalStockPcs', N'DECIMAL(18,2)'),
(N'Weekly_data', N'TotalStockDdp', N'DECIMAL(18,2)'),
(N'Weekly_data', N'SalesPcs', N'DECIMAL(18,2)'),
(N'Weekly_data', N'SalesRub', N'DECIMAL(18,2)'),
(N'Weekly_data', N'Revenue', N'DECIMAL(18,2)'),
(N'Weekly_data', N'Gp', N'DECIMAL(18,2)'),
(N'Weekly_data', N'DiscountTotalRub', N'DECIMAL(18,2)'),
(N'Weekly_data_stage', N'TotalStockPcs', N'DECIMAL(18,2)'),
(N'Weekly_data_stage', N'TotalStockDdp', N'DECIMAL(18,2)'),
(N'Weekly_data_stage', N'SalesPcs', N'DECIMAL(18,2)'),
(N'Weekly_data_stage', N'SalesRub', N'DECIMAL(18,2)'),
(N'Weekly_data_stage', N'Revenue', N'DECIMAL(18,2)'),
(N'Weekly_data_stage', N'Gp', N'DECIMAL(18,2)'),
(N'Weekly_data_stage', N'DiscountTotalRub', N'DECIMAL(18,2)');

DECLARE @TableName SYSNAME, @ColumnName SYSNAME, @SqlType NVARCHAR(50), @Sql NVARCHAR(MAX);
DECLARE WeeklyCursor CURSOR LOCAL FAST_FORWARD FOR
    SELECT TableName, ColumnName, SqlType FROM @WeeklyMetrics;
OPEN WeeklyCursor;
FETCH NEXT FROM WeeklyCursor INTO @TableName, @ColumnName, @SqlType;
WHILE @@FETCH_STATUS = 0
BEGIN
    IF EXISTS (
        SELECT 1
        FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.' + @TableName)
          AND name = @ColumnName
          AND is_nullable = 1
    )
    BEGIN
        SET @Sql = N'UPDATE dbo.' + QUOTENAME(@TableName)
                 + N' SET ' + QUOTENAME(@ColumnName) + N' = 0 WHERE '
                 + QUOTENAME(@ColumnName) + N' IS NULL;';
        EXEC sys.sp_executesql @Sql;

        SET @Sql = N'ALTER TABLE dbo.' + QUOTENAME(@TableName)
                 + N' ALTER COLUMN ' + QUOTENAME(@ColumnName) + N' '
                 + @SqlType + N' NOT NULL;';
        EXEC sys.sp_executesql @Sql;
    END;

    FETCH NEXT FROM WeeklyCursor INTO @TableName, @ColumnName, @SqlType;
END;
CLOSE WeeklyCursor;
DEALLOCATE WeeklyCursor;
GO

/* ============================================================================
   2. CDData required delete keys + canonical indexes
   ============================================================================ */

BEGIN TRY
    BEGIN TRANSACTION;

    IF EXISTS (
        SELECT 1 FROM dbo.CD_data
        WHERE god IS NULL OR sezon IS NULL OR den IS NULL OR nazvanie IS NULL
           OR LTRIM(RTRIM(REPLACE(REPLACE(nazvanie, NCHAR(160), N' '), NCHAR(8239), N' '))) = N''
    )
    OR EXISTS (
        SELECT 1 FROM dbo.CD_data_stage
        WHERE god IS NULL OR sezon IS NULL OR den IS NULL OR nazvanie IS NULL
           OR LTRIM(RTRIM(REPLACE(REPLACE(nazvanie, NCHAR(160), N' '), NCHAR(8239), N' '))) = N''
    )
        THROW 50001, 'CDData required-key migration blocked by invalid legacy target/stage rows.', 1;

    IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'IX_CD_data_god_sezon')
       AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name IN (N'god', N'sezon') AND is_nullable = 1)
        DROP INDEX IX_CD_data_god_sezon ON dbo.CD_data;

    IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'IX_CD_data_nazvanie_den')
       AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name IN (N'nazvanie', N'den') AND is_nullable = 1)
        DROP INDEX IX_CD_data_nazvanie_den ON dbo.CD_data;

    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'nazvanie' AND is_nullable = 1)
        ALTER TABLE dbo.CD_data ALTER COLUMN nazvanie NVARCHAR(255) NOT NULL;
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'god' AND is_nullable = 1)
        ALTER TABLE dbo.CD_data ALTER COLUMN god INT NOT NULL;
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'sezon' AND is_nullable = 1)
        ALTER TABLE dbo.CD_data ALTER COLUMN sezon INT NOT NULL;
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'den' AND is_nullable = 1)
        ALTER TABLE dbo.CD_data ALTER COLUMN den INT NOT NULL;

    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data_stage') AND name = N'nazvanie' AND is_nullable = 1)
        ALTER TABLE dbo.CD_data_stage ALTER COLUMN nazvanie NVARCHAR(255) NOT NULL;
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data_stage') AND name = N'god' AND is_nullable = 1)
        ALTER TABLE dbo.CD_data_stage ALTER COLUMN god INT NOT NULL;
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data_stage') AND name = N'sezon' AND is_nullable = 1)
        ALTER TABLE dbo.CD_data_stage ALTER COLUMN sezon INT NOT NULL;
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data_stage') AND name = N'den' AND is_nullable = 1)
        ALTER TABLE dbo.CD_data_stage ALTER COLUMN den INT NOT NULL;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'IX_CD_data_god_sezon')
        CREATE NONCLUSTERED INDEX IX_CD_data_god_sezon ON dbo.CD_data(god, sezon);

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'IX_CD_data_nazvanie_den')
        CREATE NONCLUSTERED INDEX IX_CD_data_nazvanie_den ON dbo.CD_data(nazvanie, den);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

/* ============================================================================
   3. CDData zero-metric contract
   ============================================================================ */

DECLARE @CDMetrics TABLE (TableName SYSNAME NOT NULL, ColumnName SYSNAME NOT NULL, SqlType NVARCHAR(50) NOT NULL);
INSERT INTO @CDMetrics(TableName, ColumnName, SqlType)
VALUES
(N'CD_data',N'stock_start_pcs',N'DECIMAL(18,2)'),(N'CD_data',N'stock_start_dd',N'DECIMAL(18,2)'),
(N'CD_data',N'sales_pcs',N'DECIMAL(18,2)'),(N'CD_data',N'sales_rub',N'DECIMAL(18,2)'),
(N'CD_data',N'revenue',N'DECIMAL(18,2)'),(N'CD_data',N'gp',N'DECIMAL(18,2)'),
(N'CD_data',N'cogs',N'DECIMAL(18,2)'),(N'CD_data',N'sales_frp_price',N'DECIMAL(18,2)'),
(N'CD_data',N'sales_discount',N'DECIMAL(18,2)'),(N'CD_data',N'stock_stores_pcs',N'DECIMAL(18,2)'),
(N'CD_data',N'stock_stores_dd',N'DECIMAL(18,2)'),(N'CD_data',N'plan_rub',N'INT'),
(N'CD_data_stage',N'stock_start_pcs',N'DECIMAL(18,2)'),(N'CD_data_stage',N'stock_start_dd',N'DECIMAL(18,2)'),
(N'CD_data_stage',N'sales_pcs',N'DECIMAL(18,2)'),(N'CD_data_stage',N'sales_rub',N'DECIMAL(18,2)'),
(N'CD_data_stage',N'revenue',N'DECIMAL(18,2)'),(N'CD_data_stage',N'gp',N'DECIMAL(18,2)'),
(N'CD_data_stage',N'cogs',N'DECIMAL(18,2)'),(N'CD_data_stage',N'sales_frp_price',N'DECIMAL(18,2)'),
(N'CD_data_stage',N'sales_discount',N'DECIMAL(18,2)'),(N'CD_data_stage',N'stock_stores_pcs',N'DECIMAL(18,2)'),
(N'CD_data_stage',N'stock_stores_dd',N'DECIMAL(18,2)'),(N'CD_data_stage',N'plan_rub',N'INT');

DECLARE @CDTable SYSNAME, @CDColumn SYSNAME, @CDType NVARCHAR(50), @CDSql NVARCHAR(MAX);
DECLARE CDCursor CURSOR LOCAL FAST_FORWARD FOR SELECT TableName, ColumnName, SqlType FROM @CDMetrics;
OPEN CDCursor;
FETCH NEXT FROM CDCursor INTO @CDTable, @CDColumn, @CDType;
WHILE @@FETCH_STATUS = 0
BEGIN
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.' + @CDTable) AND name = @CDColumn AND is_nullable = 1)
    BEGIN
        SET @CDSql = N'UPDATE dbo.' + QUOTENAME(@CDTable) + N' SET ' + QUOTENAME(@CDColumn) + N' = 0 WHERE ' + QUOTENAME(@CDColumn) + N' IS NULL;';
        EXEC sys.sp_executesql @CDSql;
        SET @CDSql = N'ALTER TABLE dbo.' + QUOTENAME(@CDTable) + N' ALTER COLUMN ' + QUOTENAME(@CDColumn) + N' ' + @CDType + N' NOT NULL;';
        EXEC sys.sp_executesql @CDSql;
    END;
    FETCH NEXT FROM CDCursor INTO @CDTable, @CDColumn, @CDType;
END;
CLOSE CDCursor;
DEALLOCATE CDCursor;
GO

/* ============================================================================
   4. CDEcom required delete keys + canonical indexes
   ============================================================================ */

BEGIN TRY
    BEGIN TRANSACTION;

    IF EXISTS (
        SELECT 1 FROM dbo.CD_ecom
        WHERE [year] IS NULL OR season IS NULL OR [day] IS NULL OR name IS NULL
           OR LTRIM(RTRIM(REPLACE(REPLACE(name, NCHAR(160), N' '), NCHAR(8239), N' '))) = N''
    )
    OR EXISTS (
        SELECT 1 FROM dbo.CD_ecom_stage
        WHERE [year] IS NULL OR season IS NULL OR [day] IS NULL OR name IS NULL
           OR LTRIM(RTRIM(REPLACE(REPLACE(name, NCHAR(160), N' '), NCHAR(8239), N' '))) = N''
    )
        THROW 50002, 'CDEcom required-key migration blocked by invalid legacy target/stage rows.', 1;

    IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'IX_CD_ecom_year_season')
       AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name IN (N'year', N'season') AND is_nullable = 1)
        DROP INDEX IX_CD_ecom_year_season ON dbo.CD_ecom;

    IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'IX_CD_ecom_name_day')
       AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name IN (N'name', N'day') AND is_nullable = 1)
        DROP INDEX IX_CD_ecom_name_day ON dbo.CD_ecom;

    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name=N'name' AND is_nullable=1)
        ALTER TABLE dbo.CD_ecom ALTER COLUMN name NVARCHAR(255) NOT NULL;
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name=N'year' AND is_nullable=1)
        ALTER TABLE dbo.CD_ecom ALTER COLUMN [year] INT NOT NULL;
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name=N'season' AND is_nullable=1)
        ALTER TABLE dbo.CD_ecom ALTER COLUMN season INT NOT NULL;
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name=N'day' AND is_nullable=1)
        ALTER TABLE dbo.CD_ecom ALTER COLUMN [day] INT NOT NULL;

    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name=N'name' AND is_nullable=1)
        ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN name NVARCHAR(255) NOT NULL;
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name=N'year' AND is_nullable=1)
        ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN [year] INT NOT NULL;
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name=N'season' AND is_nullable=1)
        ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN season INT NOT NULL;
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name=N'day' AND is_nullable=1)
        ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN [day] INT NOT NULL;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name=N'IX_CD_ecom_year_season')
        CREATE NONCLUSTERED INDEX IX_CD_ecom_year_season ON dbo.CD_ecom([year], season);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name=N'IX_CD_ecom_name_day')
        CREATE NONCLUSTERED INDEX IX_CD_ecom_name_day ON dbo.CD_ecom(name, [day]);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

/* ============================================================================
   5. CDEcom zero-metric contract
   ============================================================================ */

DECLARE @EcomMetrics TABLE (TableName SYSNAME NOT NULL, ColumnName SYSNAME NOT NULL, SqlType NVARCHAR(50) NOT NULL);
INSERT INTO @EcomMetrics(TableName, ColumnName, SqlType)
VALUES
(N'CD_ecom',N'orderPcs',N'DECIMAL(18,2)'),(N'CD_ecom',N'orderRub',N'DECIMAL(18,2)'),
(N'CD_ecom',N'foundPcs',N'DECIMAL(18,2)'),(N'CD_ecom',N'foundRub',N'DECIMAL(18,2)'),
(N'CD_ecom',N'salesPcs',N'DECIMAL(18,2)'),(N'CD_ecom',N'salesRub',N'DECIMAL(18,2)'),
(N'CD_ecom',N'revenue',N'DECIMAL(18,2)'),(N'CD_ecom',N'gp',N'DECIMAL(18,2)'),
(N'CD_ecom',N'cogs',N'DECIMAL(18,2)'),(N'CD_ecom',N'salesDiscount',N'DECIMAL(18,2)'),
(N'CD_ecom',N'planRub',N'BIGINT'),(N'CD_ecom',N'stockStoresPcs',N'BIGINT'),(N'CD_ecom',N'stockStoresDdp',N'BIGINT'),
(N'CD_ecom_stage',N'orderPcs',N'DECIMAL(18,2)'),(N'CD_ecom_stage',N'orderRub',N'DECIMAL(18,2)'),
(N'CD_ecom_stage',N'foundPcs',N'DECIMAL(18,2)'),(N'CD_ecom_stage',N'foundRub',N'DECIMAL(18,2)'),
(N'CD_ecom_stage',N'salesPcs',N'DECIMAL(18,2)'),(N'CD_ecom_stage',N'salesRub',N'DECIMAL(18,2)'),
(N'CD_ecom_stage',N'revenue',N'DECIMAL(18,2)'),(N'CD_ecom_stage',N'gp',N'DECIMAL(18,2)'),
(N'CD_ecom_stage',N'cogs',N'DECIMAL(18,2)'),(N'CD_ecom_stage',N'salesDiscount',N'DECIMAL(18,2)'),
(N'CD_ecom_stage',N'planRub',N'BIGINT'),(N'CD_ecom_stage',N'stockStoresPcs',N'BIGINT'),(N'CD_ecom_stage',N'stockStoresDdp',N'BIGINT');

DECLARE @ETable SYSNAME, @EColumn SYSNAME, @EType NVARCHAR(50), @ESql NVARCHAR(MAX);
DECLARE ECursor CURSOR LOCAL FAST_FORWARD FOR SELECT TableName, ColumnName, SqlType FROM @EcomMetrics;
OPEN ECursor;
FETCH NEXT FROM ECursor INTO @ETable, @EColumn, @EType;
WHILE @@FETCH_STATUS = 0
BEGIN
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.' + @ETable) AND name=@EColumn AND is_nullable=1)
    BEGIN
        SET @ESql = N'UPDATE dbo.' + QUOTENAME(@ETable) + N' SET ' + QUOTENAME(@EColumn) + N' = 0 WHERE ' + QUOTENAME(@EColumn) + N' IS NULL;';
        EXEC sys.sp_executesql @ESql;
        SET @ESql = N'ALTER TABLE dbo.' + QUOTENAME(@ETable) + N' ALTER COLUMN ' + QUOTENAME(@EColumn) + N' ' + @EType + N' NOT NULL;';
        EXEC sys.sp_executesql @ESql;
    END;
    FETCH NEXT FROM ECursor INTO @ETable, @EColumn, @EType;
END;
CLOSE ECursor;
DEALLOCATE ECursor;
GO

/* ============================================================================
   6. Deletion metadata
   ============================================================================ */

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteMonth') IS NULL
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD DeleteMonth INT NULL;
GO
IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteYearText') IS NULL
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD DeleteYearText NVARCHAR(50) NULL;
GO
IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteMonthText') IS NULL
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD DeleteMonthText NVARCHAR(50) NULL;
GO
IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteCriterion') IS NULL
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD DeleteCriterion NVARCHAR(50) NULL;
GO
IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteParameter1Name') IS NULL
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD DeleteParameter1Name NVARCHAR(50) NULL;
GO
IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteParameter1Value') IS NULL
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD DeleteParameter1Value NVARCHAR(1000) NULL;
GO
IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteParameter2Name') IS NULL
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD DeleteParameter2Name NVARCHAR(50) NULL;
GO
IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteParameter2Value') IS NULL
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD DeleteParameter2Value NVARCHAR(1000) NULL;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.DWH_Excel_Load_Error')
      AND name = N'IX_DWH_Excel_Load_Error_LoadSessionId'
)
    CREATE NONCLUSTERED INDEX IX_DWH_Excel_Load_Error_LoadSessionId
        ON dbo.DWH_Excel_Load_Error(LoadSessionId);
GO

/* ============================================================================
   7. StoreTurnover v2 coexistence migration
   ============================================================================ */

IF OBJECT_ID(N'dbo.StoreTurnover', N'U') IS NULL
    THROW 51000, 'dbo.StoreTurnover must exist before StoreTurnover v2 migration', 1;
GO

IF COL_LENGTH(N'dbo.StoreTurnover', N'SKU') IS NULL
    THROW 51001, 'dbo.StoreTurnover.SKU is required for StoreTurnover v2 migration', 1;
GO

DECLARE @SkuType SYSNAME, @SkuMaxLength SMALLINT, @SkuNullable BIT;
SELECT @SkuType = TYPE_NAME(system_type_id), @SkuMaxLength = max_length, @SkuNullable = is_nullable
FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.StoreTurnover') AND name = N'SKU';
IF @SkuType NOT IN (N'varchar', N'nvarchar')
    THROW 51002, 'dbo.StoreTurnover.SKU has an incompatible SQL type', 1;
IF @SkuType = N'varchar' OR @SkuMaxLength < 510 OR @SkuNullable = 0
BEGIN
    DECLARE @SkuTargetLength NVARCHAR(20) = CASE
        WHEN @SkuMaxLength = -1 THEN N'MAX'
        WHEN @SkuType = N'varchar' AND @SkuMaxLength > 4000 THEN N'MAX'
        WHEN @SkuType = N'nvarchar' AND @SkuMaxLength / 2 > 255 THEN CONVERT(NVARCHAR(20), @SkuMaxLength / 2)
        WHEN @SkuType = N'varchar' AND @SkuMaxLength > 255 THEN CONVERT(NVARCHAR(20), @SkuMaxLength)
        ELSE N'255' END;
    DECLARE @SkuAlterSql NVARCHAR(MAX);
    SET @SkuAlterSql = N'ALTER TABLE dbo.StoreTurnover ALTER COLUMN SKU NVARCHAR(' + @SkuTargetLength + N') NULL';
    EXEC sys.sp_executesql @SkuAlterSql;
END;
GO

IF COL_LENGTH(N'dbo.StoreTurnover', N'StoreRus') IS NULL
    THROW 51003, 'dbo.StoreTurnover.StoreRus is required for StoreTurnover v2 migration', 1;
GO
DECLARE @StoreRusType SYSNAME, @StoreRusMaxLength SMALLINT, @StoreRusNullable BIT;
SELECT @StoreRusType = TYPE_NAME(system_type_id), @StoreRusMaxLength = max_length, @StoreRusNullable = is_nullable
FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.StoreTurnover') AND name = N'StoreRus';
IF @StoreRusType NOT IN (N'varchar', N'nvarchar')
    THROW 51004, 'dbo.StoreTurnover.StoreRus has an incompatible SQL type', 1;
IF @StoreRusType = N'varchar' OR @StoreRusMaxLength < 510 OR @StoreRusNullable = 0
BEGIN
    DECLARE @StoreRusTargetLength NVARCHAR(20) = CASE
        WHEN @StoreRusMaxLength = -1 THEN N'MAX'
        WHEN @StoreRusType = N'varchar' AND @StoreRusMaxLength > 4000 THEN N'MAX'
        WHEN @StoreRusType = N'nvarchar' AND @StoreRusMaxLength / 2 > 255 THEN CONVERT(NVARCHAR(20), @StoreRusMaxLength / 2)
        WHEN @StoreRusType = N'varchar' AND @StoreRusMaxLength > 255 THEN CONVERT(NVARCHAR(20), @StoreRusMaxLength)
        ELSE N'255' END;
    DECLARE @StoreRusAlterSql NVARCHAR(MAX);
    SET @StoreRusAlterSql = N'ALTER TABLE dbo.StoreTurnover ALTER COLUMN StoreRus NVARCHAR(' + @StoreRusTargetLength + N') NULL';
    EXEC sys.sp_executesql @StoreRusAlterSql;
END;
GO

IF OBJECT_ID(N'dbo.StoreTurnover_raw', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.StoreTurnover_raw (
        Id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        LoadSessionId BIGINT NOT NULL,
        ExcelRowNum BIGINT NULL,
        sku NVARCHAR(4000) NULL,
        period NVARCHAR(4000) NULL,
        storeRus NVARCHAR(4000) NULL,
        remainingSum NVARCHAR(4000) NULL,
        remainingDays NVARCHAR(4000) NULL,
        salesQuantity NVARCHAR(4000) NULL,
        sales NVARCHAR(4000) NULL,
        asp NVARCHAR(4000) NULL,
        revenue NVARCHAR(4000) NULL,
        gp NVARCHAR(4000) NULL,
        discountTotal NVARCHAR(4000) NULL,
        CreatedAt DATETIME2(0) NOT NULL CONSTRAINT DF_StoreTurnover_raw_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT FK_StoreTurnover_raw_LoadSession FOREIGN KEY (LoadSessionId) REFERENCES dbo.DWH_Excel_Load_Session(Id)
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
        remainingSum INT NOT NULL,
        remainingDays INT NOT NULL,
        salesQuantity INT NOT NULL,
        sales INT NOT NULL,
        asp INT NOT NULL,
        revenue INT NOT NULL,
        gp INT NOT NULL,
        discountTotal INT NOT NULL,
        RawRowId BIGINT NOT NULL,
        CONSTRAINT FK_StoreTurnover_stage_LoadSession FOREIGN KEY (LoadSessionId) REFERENCES dbo.DWH_Excel_Load_Session(Id),
        CONSTRAINT FK_StoreTurnover_stage_RawRow FOREIGN KEY (RawRowId) REFERENCES dbo.StoreTurnover_raw(Id)
    );
END;
GO

IF COL_LENGTH(N'dbo.StoreTurnover', N'LoadSessionId') IS NULL
    ALTER TABLE dbo.StoreTurnover ADD LoadSessionId BIGINT NULL;
GO
IF COL_LENGTH(N'dbo.StoreTurnover', N'RawRowId') IS NULL
    ALTER TABLE dbo.StoreTurnover ADD RawRowId BIGINT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID(N'dbo.StoreTurnover') AND name=N'FK_StoreTurnover_LoadSession')
    ALTER TABLE dbo.StoreTurnover WITH CHECK ADD CONSTRAINT FK_StoreTurnover_LoadSession FOREIGN KEY (LoadSessionId) REFERENCES dbo.DWH_Excel_Load_Session(Id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.StoreTurnover') AND name=N'IX_StoreTurnover_LoadSessionId')
    CREATE INDEX IX_StoreTurnover_LoadSessionId ON dbo.StoreTurnover(LoadSessionId);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.StoreTurnover_raw') AND name=N'IX_StoreTurnover_raw_LoadSessionId_Id')
    CREATE INDEX IX_StoreTurnover_raw_LoadSessionId_Id ON dbo.StoreTurnover_raw(LoadSessionId, Id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.StoreTurnover_stage') AND name=N'IX_StoreTurnover_stage_LoadSessionId')
    CREATE INDEX IX_StoreTurnover_stage_LoadSessionId ON dbo.StoreTurnover_stage(LoadSessionId);
GO

PRINT N'Production migration completed. Run 02_verify.sql before deployment.';
GO
