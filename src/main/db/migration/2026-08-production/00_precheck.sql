USE [ReplenishmentDWH];
GO

SET NOCOUNT ON;
GO

/* 1. Required-key violations: CDData */
SELECT
    N'CD_data' AS TableName,
    Id,
    LoadSessionId,
    RawRowId,
    nazvanie,
    god,
    sezon,
    den
FROM dbo.CD_data
WHERE god IS NULL
   OR sezon IS NULL
   OR den IS NULL
   OR nazvanie IS NULL
   OR LTRIM(RTRIM(REPLACE(REPLACE(nazvanie, NCHAR(160), N' '), NCHAR(8239), N' '))) = N'';
GO

SELECT
    N'CD_data_stage' AS TableName,
    LoadSessionId,
    RawRowId,
    nazvanie,
    god,
    sezon,
    den
FROM dbo.CD_data_stage
WHERE god IS NULL
   OR sezon IS NULL
   OR den IS NULL
   OR nazvanie IS NULL
   OR LTRIM(RTRIM(REPLACE(REPLACE(nazvanie, NCHAR(160), N' '), NCHAR(8239), N' '))) = N'';
GO

/* 2. Required-key violations: CDEcom */
SELECT
    N'CD_ecom' AS TableName,
    Id,
    LoadSessionId,
    RawRowId,
    name,
    [year],
    season,
    [day]
FROM dbo.CD_ecom
WHERE [year] IS NULL
   OR season IS NULL
   OR [day] IS NULL
   OR name IS NULL
   OR LTRIM(RTRIM(REPLACE(REPLACE(name, NCHAR(160), N' '), NCHAR(8239), N' '))) = N'';
GO

SELECT
    N'CD_ecom_stage' AS TableName,
    LoadSessionId,
    RawRowId,
    name,
    [year],
    season,
    [day]
FROM dbo.CD_ecom_stage
WHERE [year] IS NULL
   OR season IS NULL
   OR [day] IS NULL
   OR name IS NULL
   OR LTRIM(RTRIM(REPLACE(REPLACE(name, NCHAR(160), N' '), NCHAR(8239), N' '))) = N'';
GO

/* 3. Zero-metric NULL counts before migration */
SELECT
    N'Weekly_data' AS TableName,
    SUM(CASE WHEN TotalStockPcs IS NULL THEN 1 ELSE 0 END) AS TotalStockPcsNulls,
    SUM(CASE WHEN TotalStockDdp IS NULL THEN 1 ELSE 0 END) AS TotalStockDdpNulls,
    SUM(CASE WHEN SalesPcs IS NULL THEN 1 ELSE 0 END) AS SalesPcsNulls,
    SUM(CASE WHEN SalesRub IS NULL THEN 1 ELSE 0 END) AS SalesRubNulls,
    SUM(CASE WHEN Revenue IS NULL THEN 1 ELSE 0 END) AS RevenueNulls,
    SUM(CASE WHEN Gp IS NULL THEN 1 ELSE 0 END) AS GpNulls,
    SUM(CASE WHEN DiscountTotalRub IS NULL THEN 1 ELSE 0 END) AS DiscountTotalRubNulls
FROM dbo.Weekly_data;
GO

SELECT
    N'CD_data' AS TableName,
    SUM(CASE WHEN stock_start_pcs IS NULL THEN 1 ELSE 0 END) AS stock_start_pcsNulls,
    SUM(CASE WHEN stock_start_dd IS NULL THEN 1 ELSE 0 END) AS stock_start_ddNulls,
    SUM(CASE WHEN sales_pcs IS NULL THEN 1 ELSE 0 END) AS sales_pcsNulls,
    SUM(CASE WHEN sales_rub IS NULL THEN 1 ELSE 0 END) AS sales_rubNulls,
    SUM(CASE WHEN revenue IS NULL THEN 1 ELSE 0 END) AS revenueNulls,
    SUM(CASE WHEN gp IS NULL THEN 1 ELSE 0 END) AS gpNulls,
    SUM(CASE WHEN cogs IS NULL THEN 1 ELSE 0 END) AS cogsNulls,
    SUM(CASE WHEN sales_frp_price IS NULL THEN 1 ELSE 0 END) AS sales_frp_priceNulls,
    SUM(CASE WHEN sales_discount IS NULL THEN 1 ELSE 0 END) AS sales_discountNulls,
    SUM(CASE WHEN stock_stores_pcs IS NULL THEN 1 ELSE 0 END) AS stock_stores_pcsNulls,
    SUM(CASE WHEN stock_stores_dd IS NULL THEN 1 ELSE 0 END) AS stock_stores_ddNulls,
    SUM(CASE WHEN plan_rub IS NULL THEN 1 ELSE 0 END) AS plan_rubNulls
FROM dbo.CD_data;
GO

SELECT
    N'CD_ecom' AS TableName,
    SUM(CASE WHEN orderPcs IS NULL THEN 1 ELSE 0 END) AS orderPcsNulls,
    SUM(CASE WHEN orderRub IS NULL THEN 1 ELSE 0 END) AS orderRubNulls,
    SUM(CASE WHEN foundPcs IS NULL THEN 1 ELSE 0 END) AS foundPcsNulls,
    SUM(CASE WHEN foundRub IS NULL THEN 1 ELSE 0 END) AS foundRubNulls,
    SUM(CASE WHEN salesPcs IS NULL THEN 1 ELSE 0 END) AS salesPcsNulls,
    SUM(CASE WHEN salesRub IS NULL THEN 1 ELSE 0 END) AS salesRubNulls,
    SUM(CASE WHEN revenue IS NULL THEN 1 ELSE 0 END) AS revenueNulls,
    SUM(CASE WHEN gp IS NULL THEN 1 ELSE 0 END) AS gpNulls,
    SUM(CASE WHEN cogs IS NULL THEN 1 ELSE 0 END) AS cogsNulls,
    SUM(CASE WHEN salesDiscount IS NULL THEN 1 ELSE 0 END) AS salesDiscountNulls,
    SUM(CASE WHEN planRub IS NULL THEN 1 ELSE 0 END) AS planRubNulls,
    SUM(CASE WHEN stockStoresPcs IS NULL THEN 1 ELSE 0 END) AS stockStoresPcsNulls,
    SUM(CASE WHEN stockStoresDdp IS NULL THEN 1 ELSE 0 END) AS stockStoresDdpNulls
FROM dbo.CD_ecom;
GO

/* 4. StoreTurnover legacy shape */
SELECT
    c.column_id,
    c.name AS ColumnName,
    TYPE_NAME(c.user_type_id) AS DataType,
    c.max_length,
    c.precision,
    c.scale,
    c.is_nullable
FROM sys.columns c
WHERE c.object_id = OBJECT_ID(N'dbo.StoreTurnover')
ORDER BY c.column_id;
GO
