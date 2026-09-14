USE [ReplenishmentDWH];
GO

SET NOCOUNT ON;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.columns c
    WHERE c.object_id = OBJECT_ID(N'dbo.CD_data')
      AND c.name = N'plan_rub'
      AND TYPE_NAME(c.user_type_id) = N'decimal'
      AND c.precision = 18
      AND c.scale = 2
      AND c.is_nullable = 0
)
    THROW 52021, 'Verification failed: dbo.CD_data.plan_rub is not DECIMAL(18,2) NOT NULL.', 1;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.columns c
    WHERE c.object_id = OBJECT_ID(N'dbo.CD_data_stage')
      AND c.name = N'plan_rub'
      AND TYPE_NAME(c.user_type_id) = N'decimal'
      AND c.precision = 18
      AND c.scale = 2
      AND c.is_nullable = 0
)
    THROW 52022, 'Verification failed: dbo.CD_data_stage.plan_rub is not DECIMAL(18,2) NOT NULL.', 1;
GO

SELECT
    OBJECT_SCHEMA_NAME(c.object_id) AS SchemaName,
    OBJECT_NAME(c.object_id) AS TableName,
    c.name AS ColumnName,
    TYPE_NAME(c.user_type_id) AS DataType,
    c.precision,
    c.scale,
    c.is_nullable
FROM sys.columns c
WHERE c.object_id IN (OBJECT_ID(N'dbo.CD_data'), OBJECT_ID(N'dbo.CD_data_stage'))
  AND c.name = N'plan_rub'
ORDER BY TableName;
GO
