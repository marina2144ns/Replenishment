USE [ReplenishmentDWH];
GO

SET NOCOUNT ON;
GO

IF OBJECT_ID(N'dbo.CD_data', N'U') IS NULL
    THROW 52001, 'Precheck failed: dbo.CD_data does not exist.', 1;
GO

IF OBJECT_ID(N'dbo.CD_data_stage', N'U') IS NULL
    THROW 52002, 'Precheck failed: dbo.CD_data_stage does not exist.', 1;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.columns c
    WHERE c.object_id = OBJECT_ID(N'dbo.CD_data')
      AND c.name = N'plan_rub'
      AND c.is_nullable = 0
      AND (
          TYPE_NAME(c.user_type_id) = N'int'
          OR (
              TYPE_NAME(c.user_type_id) = N'decimal'
              AND c.precision = 18
              AND c.scale = 2
          )
      )
)
    THROW 52003, 'Precheck failed: dbo.CD_data.plan_rub must be INT NOT NULL or DECIMAL(18,2) NOT NULL.', 1;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.columns c
    WHERE c.object_id = OBJECT_ID(N'dbo.CD_data_stage')
      AND c.name = N'plan_rub'
      AND c.is_nullable = 0
      AND (
          TYPE_NAME(c.user_type_id) = N'int'
          OR (
              TYPE_NAME(c.user_type_id) = N'decimal'
              AND c.precision = 18
              AND c.scale = 2
          )
      )
)
    THROW 52004, 'Precheck failed: dbo.CD_data_stage.plan_rub must be INT NOT NULL or DECIMAL(18,2) NOT NULL.', 1;
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
