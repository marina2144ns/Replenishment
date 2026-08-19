USE [ReplenishmentDWH];
GO

SET NOCOUNT ON;
GO

/* ============================================================================
   1. Canonical target/stage contracts
   ============================================================================ */

SELECT
    t.name AS TableName,
    c.column_id AS ColumnId,
    c.name AS ColumnName,
    TYPE_NAME(c.user_type_id) AS DataType,
    CASE WHEN TYPE_NAME(c.user_type_id) IN (N'nvarchar', N'nchar') THEN c.max_length / 2 ELSE c.max_length END AS MaxLength,
    c.precision,
    c.scale,
    c.is_nullable
FROM sys.tables t
JOIN sys.columns c ON c.object_id = t.object_id
WHERE
       (t.name IN (N'Weekly_data', N'Weekly_data_stage')
        AND c.name IN (N'Year',N'Week',N'TotalStockPcs',N'TotalStockDdp',N'SalesPcs',N'SalesRub',N'Revenue',N'Gp',N'DiscountTotalRub'))
    OR (t.name IN (N'CD_data', N'CD_data_stage')
        AND c.name IN (N'nazvanie',N'god',N'sezon',N'den',N'stock_start_pcs',N'stock_start_dd',N'sales_pcs',N'sales_rub',N'revenue',N'gp',N'cogs',N'sales_frp_price',N'sales_discount',N'stock_stores_pcs',N'stock_stores_dd',N'plan_rub'))
    OR (t.name IN (N'CD_ecom', N'CD_ecom_stage')
        AND c.name IN (N'name',N'year',N'season',N'day',N'orderPcs',N'orderRub',N'foundPcs',N'foundRub',N'salesPcs',N'salesRub',N'revenue',N'gp',N'cogs',N'salesDiscount',N'planRub',N'stockStoresPcs',N'stockStoresDdp'))
ORDER BY t.name, c.column_id;
GO

/* ============================================================================
   2. Deletion metadata
   ============================================================================ */

SELECT
    c.column_id,
    c.name AS ColumnName,
    TYPE_NAME(c.user_type_id) AS DataType,
    CASE WHEN TYPE_NAME(c.user_type_id) IN (N'nvarchar', N'nchar') THEN c.max_length / 2 ELSE c.max_length END AS MaxLength,
    c.is_nullable
FROM sys.columns c
WHERE c.object_id = OBJECT_ID(N'dbo.DWH_Excel_Load_Session')
  AND c.name IN (
      N'OperationType', N'OperationMode', N'DeleteYear', N'DeleteWeek', N'DeleteMonth',
      N'DeleteYearText', N'DeleteMonthText', N'SourceLoadSessionId',
      N'DeleteCriterion', N'DeleteParameter1Name', N'DeleteParameter1Value',
      N'DeleteParameter2Name', N'DeleteParameter2Value', N'DeletedRows'
  )
ORDER BY c.column_id;
GO

/* ============================================================================
   3. Required indexes
   ============================================================================ */

SELECT
    t.name AS TableName,
    i.name AS IndexName,
    i.type_desc AS IndexType,
    i.is_unique,
    i.is_disabled
FROM sys.indexes i
JOIN sys.tables t ON t.object_id = i.object_id
WHERE
       (t.name = N'Weekly_data' AND i.name IN (N'IX_Weekly_data_LoadSessionId',N'IX_Weekly_data_Year_Week'))
    OR (t.name = N'Weekly_data_raw' AND i.name = N'IX_Weekly_data_raw_LoadSessionId')
    OR (t.name = N'Weekly_data_stage' AND i.name = N'IX_Weekly_data_stage_LoadSessionId')
    OR (t.name = N'CD_data' AND i.name IN (N'IX_CD_data_LoadSessionId',N'IX_CD_data_god_sezon',N'IX_CD_data_nazvanie_den'))
    OR (t.name = N'CD_data_raw' AND i.name = N'IX_CD_data_raw_LoadSessionId')
    OR (t.name = N'CD_data_stage' AND i.name = N'IX_CD_data_stage_LoadSessionId')
    OR (t.name = N'CD_ecom' AND i.name IN (N'IX_CD_ecom_LoadSessionId',N'IX_CD_ecom_year_season',N'IX_CD_ecom_name_day'))
    OR (t.name = N'CD_ecom_raw' AND i.name = N'IX_CD_ecom_raw_LoadSessionId')
    OR (t.name = N'CD_ecom_stage' AND i.name = N'IX_CD_ecom_stage_LoadSessionId')
    OR (t.name = N'SalesByChannel' AND i.name IN (N'IX_SalesByChannel_LoadSessionId',N'IX_SalesByChannel_year_month'))
    OR (t.name = N'SalesByChannel_raw' AND i.name = N'IX_SalesByChannel_raw_LoadSessionId')
    OR (t.name = N'SalesByChannel_stage' AND i.name = N'IX_SalesByChannel_stage_LoadSessionId')
    OR (t.name = N'DWH_Excel_Load_Error' AND i.name = N'IX_DWH_Excel_Load_Error_LoadSessionId')
    OR (t.name = N'StoreTurnover' AND i.name = N'IX_StoreTurnover_LoadSessionId')
    OR (t.name = N'StoreTurnover_raw' AND i.name = N'IX_StoreTurnover_raw_LoadSessionId_Id')
    OR (t.name = N'StoreTurnover_stage' AND i.name = N'IX_StoreTurnover_stage_LoadSessionId')
ORDER BY t.name, i.name;
GO

/* ============================================================================
   4. StoreTurnover v2 columns
   ============================================================================ */

SELECT
    t.name AS TableName,
    c.column_id AS ColumnId,
    c.name AS ColumnName,
    TYPE_NAME(c.user_type_id) AS DataType,
    CASE WHEN TYPE_NAME(c.user_type_id) IN (N'nvarchar', N'nchar') THEN c.max_length / 2 ELSE c.max_length END AS MaxLength,
    c.precision,
    c.scale,
    c.is_nullable
FROM sys.tables t
JOIN sys.columns c ON c.object_id = t.object_id
WHERE t.name IN (N'StoreTurnover',N'StoreTurnover_raw',N'StoreTurnover_stage')
ORDER BY t.name, c.column_id;
GO

/* ============================================================================
   5. StoreTurnover v2 foreign keys
   ============================================================================ */

SELECT
    OBJECT_NAME(fk.parent_object_id) AS TableName,
    fk.name AS ForeignKeyName,
    OBJECT_NAME(fk.referenced_object_id) AS ReferencedTable,
    fk.is_disabled,
    fk.is_not_trusted
FROM sys.foreign_keys fk
WHERE OBJECT_NAME(fk.parent_object_id) IN (N'StoreTurnover',N'StoreTurnover_raw',N'StoreTurnover_stage')
ORDER BY TableName, ForeignKeyName;
GO

/* ============================================================================
   6. Remaining contract violations: must return zero rows
   ============================================================================ */

SELECT N'CD_data' AS TableName, Id, LoadSessionId, RawRowId
FROM dbo.CD_data
WHERE god IS NULL OR sezon IS NULL OR den IS NULL OR nazvanie IS NULL
   OR LTRIM(RTRIM(REPLACE(REPLACE(nazvanie, NCHAR(160), N' '), NCHAR(8239), N' '))) = N'';
GO

SELECT N'CD_ecom' AS TableName, Id, LoadSessionId, RawRowId
FROM dbo.CD_ecom
WHERE [year] IS NULL OR season IS NULL OR [day] IS NULL OR name IS NULL
   OR LTRIM(RTRIM(REPLACE(REPLACE(name, NCHAR(160), N' '), NCHAR(8239), N' '))) = N'';
GO
