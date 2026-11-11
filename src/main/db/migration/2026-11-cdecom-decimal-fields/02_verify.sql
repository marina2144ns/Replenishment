USE [ReplenishmentDWH];
GO

SET NOCOUNT ON;
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id = OBJECT_ID(N'dbo.CD_ecom') AND c.name = N'planRub' AND TYPE_NAME(c.user_type_id) = N'decimal' AND c.precision = 18 AND c.scale = 2 AND c.is_nullable = 0)
    THROW 54201, 'Verification failed: dbo.CD_ecom.planRub is not DECIMAL(18,2) NOT NULL.', 1;
IF NOT EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id = OBJECT_ID(N'dbo.CD_ecom') AND c.name = N'stockStoresPcs' AND TYPE_NAME(c.user_type_id) = N'decimal' AND c.precision = 18 AND c.scale = 2 AND c.is_nullable = 0)
    THROW 54202, 'Verification failed: dbo.CD_ecom.stockStoresPcs is not DECIMAL(18,2) NOT NULL.', 1;
IF NOT EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id = OBJECT_ID(N'dbo.CD_ecom') AND c.name = N'stockStoresDdp' AND TYPE_NAME(c.user_type_id) = N'decimal' AND c.precision = 18 AND c.scale = 2 AND c.is_nullable = 0)
    THROW 54203, 'Verification failed: dbo.CD_ecom.stockStoresDdp is not DECIMAL(18,2) NOT NULL.', 1;
IF NOT EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND c.name = N'planRub' AND TYPE_NAME(c.user_type_id) = N'decimal' AND c.precision = 18 AND c.scale = 2 AND c.is_nullable = 0)
    THROW 54204, 'Verification failed: dbo.CD_ecom_stage.planRub is not DECIMAL(18,2) NOT NULL.', 1;
IF NOT EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND c.name = N'stockStoresPcs' AND TYPE_NAME(c.user_type_id) = N'decimal' AND c.precision = 18 AND c.scale = 2 AND c.is_nullable = 0)
    THROW 54205, 'Verification failed: dbo.CD_ecom_stage.stockStoresPcs is not DECIMAL(18,2) NOT NULL.', 1;
IF NOT EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND c.name = N'stockStoresDdp' AND TYPE_NAME(c.user_type_id) = N'decimal' AND c.precision = 18 AND c.scale = 2 AND c.is_nullable = 0)
    THROW 54206, 'Verification failed: dbo.CD_ecom_stage.stockStoresDdp is not DECIMAL(18,2) NOT NULL.', 1;

SELECT
    OBJECT_SCHEMA_NAME(c.object_id) AS SchemaName,
    OBJECT_NAME(c.object_id) AS TableName,
    c.name AS ColumnName,
    TYPE_NAME(c.user_type_id) AS DataType,
    c.precision,
    c.scale,
    c.is_nullable
FROM sys.columns c
WHERE c.object_id IN (OBJECT_ID(N'dbo.CD_ecom'), OBJECT_ID(N'dbo.CD_ecom_stage'))
  AND c.name IN (N'planRub', N'stockStoresPcs', N'stockStoresDdp')
ORDER BY TableName, c.column_id;

SELECT
    N'dbo.CD_ecom' AS QualifiedTableName,
    COUNT_BIG(*) AS RowCount,
    SUM(CONVERT(DECIMAL(38,2), planRub)) AS PlanRubSum,
    SUM(CONVERT(DECIMAL(38,2), stockStoresPcs)) AS StockStoresPcsSum,
    SUM(CONVERT(DECIMAL(38,2), stockStoresDdp)) AS StockStoresDdpSum,
    SUM(CASE WHEN planRub IS NULL OR stockStoresPcs IS NULL OR stockStoresDdp IS NULL THEN 1 ELSE 0 END) AS NullContractViolations
FROM dbo.CD_ecom
UNION ALL
SELECT
    N'dbo.CD_ecom_stage',
    COUNT_BIG(*),
    SUM(CONVERT(DECIMAL(38,2), planRub)),
    SUM(CONVERT(DECIMAL(38,2), stockStoresPcs)),
    SUM(CONVERT(DECIMAL(38,2), stockStoresDdp)),
    SUM(CASE WHEN planRub IS NULL OR stockStoresPcs IS NULL OR stockStoresDdp IS NULL THEN 1 ELSE 0 END)
FROM dbo.CD_ecom_stage;
GO
