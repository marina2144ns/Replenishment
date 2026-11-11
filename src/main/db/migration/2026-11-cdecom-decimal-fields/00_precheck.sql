USE [ReplenishmentDWH];
GO

SET NOCOUNT ON;
GO

IF OBJECT_ID(N'dbo.CD_ecom', N'U') IS NULL
    THROW 54001, 'Precheck failed: dbo.CD_ecom does not exist.', 1;

IF OBJECT_ID(N'dbo.CD_ecom_stage', N'U') IS NULL
    THROW 54002, 'Precheck failed: dbo.CD_ecom_stage does not exist.', 1;

DECLARE @ExpectedColumns TABLE
(
    ObjectId INT NOT NULL,
    QualifiedTableName NVARCHAR(261) NOT NULL,
    ColumnName SYSNAME NOT NULL
);

INSERT INTO @ExpectedColumns (ObjectId, QualifiedTableName, ColumnName)
VALUES
    (OBJECT_ID(N'dbo.CD_ecom'), N'dbo.CD_ecom', N'planRub'),
    (OBJECT_ID(N'dbo.CD_ecom'), N'dbo.CD_ecom', N'stockStoresPcs'),
    (OBJECT_ID(N'dbo.CD_ecom'), N'dbo.CD_ecom', N'stockStoresDdp'),
    (OBJECT_ID(N'dbo.CD_ecom_stage'), N'dbo.CD_ecom_stage', N'planRub'),
    (OBJECT_ID(N'dbo.CD_ecom_stage'), N'dbo.CD_ecom_stage', N'stockStoresPcs'),
    (OBJECT_ID(N'dbo.CD_ecom_stage'), N'dbo.CD_ecom_stage', N'stockStoresDdp');

IF EXISTS
(
    SELECT 1
    FROM @ExpectedColumns e
    LEFT JOIN sys.columns c
      ON c.object_id = e.ObjectId
     AND c.name = e.ColumnName
    WHERE c.column_id IS NULL
       OR c.is_nullable <> 0
       OR NOT
          (
              TYPE_NAME(c.user_type_id) = N'bigint'
              OR
              (
                  TYPE_NAME(c.user_type_id) = N'decimal'
                  AND c.precision = 18
                  AND c.scale = 2
              )
          )
)
    THROW 54003, 'Precheck failed: every selected column must be BIGINT NOT NULL or DECIMAL(18,2) NOT NULL.', 1;

IF EXISTS (SELECT 1 FROM dbo.CD_ecom WHERE TRY_CONVERT(DECIMAL(18,2), planRub) IS NULL)
    THROW 54004, 'Precheck failed: dbo.CD_ecom.planRub cannot be converted to DECIMAL(18,2).', 1;
IF EXISTS (SELECT 1 FROM dbo.CD_ecom WHERE TRY_CONVERT(DECIMAL(18,2), stockStoresPcs) IS NULL)
    THROW 54005, 'Precheck failed: dbo.CD_ecom.stockStoresPcs cannot be converted to DECIMAL(18,2).', 1;
IF EXISTS (SELECT 1 FROM dbo.CD_ecom WHERE TRY_CONVERT(DECIMAL(18,2), stockStoresDdp) IS NULL)
    THROW 54006, 'Precheck failed: dbo.CD_ecom.stockStoresDdp cannot be converted to DECIMAL(18,2).', 1;
IF EXISTS (SELECT 1 FROM dbo.CD_ecom_stage WHERE TRY_CONVERT(DECIMAL(18,2), planRub) IS NULL)
    THROW 54007, 'Precheck failed: dbo.CD_ecom_stage.planRub cannot be converted to DECIMAL(18,2).', 1;
IF EXISTS (SELECT 1 FROM dbo.CD_ecom_stage WHERE TRY_CONVERT(DECIMAL(18,2), stockStoresPcs) IS NULL)
    THROW 54008, 'Precheck failed: dbo.CD_ecom_stage.stockStoresPcs cannot be converted to DECIMAL(18,2).', 1;
IF EXISTS (SELECT 1 FROM dbo.CD_ecom_stage WHERE TRY_CONVERT(DECIMAL(18,2), stockStoresDdp) IS NULL)
    THROW 54009, 'Precheck failed: dbo.CD_ecom_stage.stockStoresDdp cannot be converted to DECIMAL(18,2).', 1;

SELECT
    e.QualifiedTableName,
    c.name AS ColumnName,
    TYPE_NAME(c.user_type_id) AS DataType,
    c.precision,
    c.scale,
    c.is_nullable
FROM @ExpectedColumns e
JOIN sys.columns c
  ON c.object_id = e.ObjectId
 AND c.name = e.ColumnName
ORDER BY e.QualifiedTableName, c.column_id;

SELECT
    N'dbo.CD_ecom' AS QualifiedTableName,
    COUNT_BIG(*) AS RowCount,
    SUM(CONVERT(DECIMAL(38,2), planRub)) AS PlanRubSum,
    SUM(CONVERT(DECIMAL(38,2), stockStoresPcs)) AS StockStoresPcsSum,
    SUM(CONVERT(DECIMAL(38,2), stockStoresDdp)) AS StockStoresDdpSum
FROM dbo.CD_ecom
UNION ALL
SELECT
    N'dbo.CD_ecom_stage',
    COUNT_BIG(*),
    SUM(CONVERT(DECIMAL(38,2), planRub)),
    SUM(CONVERT(DECIMAL(38,2), stockStoresPcs)),
    SUM(CONVERT(DECIMAL(38,2), stockStoresDdp))
FROM dbo.CD_ecom_stage;

SELECT
    e.QualifiedTableName,
    e.ColumnName,
    i.name AS DependencyName,
    N'INDEX' AS DependencyType
FROM @ExpectedColumns e
JOIN sys.columns c ON c.object_id = e.ObjectId AND c.name = e.ColumnName
JOIN sys.index_columns ic ON ic.object_id = c.object_id AND ic.column_id = c.column_id
JOIN sys.indexes i ON i.object_id = ic.object_id AND i.index_id = ic.index_id
WHERE TYPE_NAME(c.user_type_id) = N'bigint'
UNION ALL
SELECT
    e.QualifiedTableName,
    e.ColumnName,
    s.name,
    N'STATISTICS'
FROM @ExpectedColumns e
JOIN sys.columns c ON c.object_id = e.ObjectId AND c.name = e.ColumnName
JOIN sys.stats_columns sc ON sc.object_id = c.object_id AND sc.column_id = c.column_id
JOIN sys.stats s ON s.object_id = sc.object_id AND s.stats_id = sc.stats_id
WHERE TYPE_NAME(c.user_type_id) = N'bigint'
UNION ALL
SELECT
    e.QualifiedTableName,
    e.ColumnName,
    OBJECT_SCHEMA_NAME(d.referencing_id) + N'.' + OBJECT_NAME(d.referencing_id),
    N'SCHEMA_BOUND_EXPRESSION'
FROM @ExpectedColumns e
JOIN sys.columns c ON c.object_id = e.ObjectId AND c.name = e.ColumnName
JOIN sys.sql_expression_dependencies d
  ON d.referenced_id = c.object_id
 AND d.referenced_minor_id = c.column_id
 AND d.is_schema_bound_reference = 1
WHERE TYPE_NAME(c.user_type_id) = N'bigint'
UNION ALL
SELECT
    e.QualifiedTableName,
    e.ColumnName,
    OBJECT_SCHEMA_NAME(dc.object_id) + N'.' + dc.name,
    N'DEFAULT_CONSTRAINT'
FROM @ExpectedColumns e
JOIN sys.columns c ON c.object_id = e.ObjectId AND c.name = e.ColumnName
JOIN sys.default_constraints dc
  ON dc.parent_object_id = c.object_id
 AND dc.parent_column_id = c.column_id
WHERE TYPE_NAME(c.user_type_id) = N'bigint'
UNION ALL
SELECT
    e.QualifiedTableName,
    e.ColumnName,
    OBJECT_SCHEMA_NAME(fk.object_id) + N'.' + fk.name,
    N'FOREIGN_KEY'
FROM @ExpectedColumns e
JOIN sys.columns c ON c.object_id = e.ObjectId AND c.name = e.ColumnName
JOIN sys.foreign_key_columns fkc
  ON (fkc.parent_object_id = c.object_id AND fkc.parent_column_id = c.column_id)
  OR (fkc.referenced_object_id = c.object_id AND fkc.referenced_column_id = c.column_id)
JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id
WHERE TYPE_NAME(c.user_type_id) = N'bigint'
UNION ALL
SELECT
    e.QualifiedTableName,
    e.ColumnName,
    OBJECT_SCHEMA_NAME(cc.object_id) + N'.' + cc.name,
    N'CHECK_CONSTRAINT'
FROM @ExpectedColumns e
JOIN sys.columns c ON c.object_id = e.ObjectId AND c.name = e.ColumnName
JOIN sys.sql_expression_dependencies d
  ON d.referenced_id = c.object_id
 AND d.referenced_minor_id = c.column_id
JOIN sys.check_constraints cc ON cc.object_id = d.referencing_id
WHERE TYPE_NAME(c.user_type_id) = N'bigint';

IF EXISTS
(
    SELECT 1
    FROM @ExpectedColumns e
    JOIN sys.columns c ON c.object_id = e.ObjectId AND c.name = e.ColumnName
    JOIN sys.index_columns ic ON ic.object_id = c.object_id AND ic.column_id = c.column_id
    WHERE TYPE_NAME(c.user_type_id) = N'bigint'
)
OR EXISTS
(
    SELECT 1
    FROM @ExpectedColumns e
    JOIN sys.columns c ON c.object_id = e.ObjectId AND c.name = e.ColumnName
    JOIN sys.stats_columns sc ON sc.object_id = c.object_id AND sc.column_id = c.column_id
    WHERE TYPE_NAME(c.user_type_id) = N'bigint'
)
OR EXISTS
(
    SELECT 1
    FROM @ExpectedColumns e
    JOIN sys.columns c ON c.object_id = e.ObjectId AND c.name = e.ColumnName
    JOIN sys.sql_expression_dependencies d
      ON d.referenced_id = c.object_id
     AND d.referenced_minor_id = c.column_id
     AND d.is_schema_bound_reference = 1
    WHERE TYPE_NAME(c.user_type_id) = N'bigint'
)
OR EXISTS
(
    SELECT 1
    FROM @ExpectedColumns e
    JOIN sys.columns c ON c.object_id = e.ObjectId AND c.name = e.ColumnName
    JOIN sys.default_constraints dc
      ON dc.parent_object_id = c.object_id
     AND dc.parent_column_id = c.column_id
    WHERE TYPE_NAME(c.user_type_id) = N'bigint'
)
OR EXISTS
(
    SELECT 1
    FROM @ExpectedColumns e
    JOIN sys.columns c ON c.object_id = e.ObjectId AND c.name = e.ColumnName
    JOIN sys.foreign_key_columns fkc
      ON (fkc.parent_object_id = c.object_id AND fkc.parent_column_id = c.column_id)
      OR (fkc.referenced_object_id = c.object_id AND fkc.referenced_column_id = c.column_id)
    WHERE TYPE_NAME(c.user_type_id) = N'bigint'
)
OR EXISTS
(
    SELECT 1
    FROM @ExpectedColumns e
    JOIN sys.columns c ON c.object_id = e.ObjectId AND c.name = e.ColumnName
    JOIN sys.sql_expression_dependencies d
      ON d.referenced_id = c.object_id
     AND d.referenced_minor_id = c.column_id
    JOIN sys.check_constraints cc ON cc.object_id = d.referencing_id
    WHERE TYPE_NAME(c.user_type_id) = N'bigint'
)
    THROW 54010, 'Precheck failed: a blocking index, statistic, constraint, or schema-bound expression depends on a selected BIGINT column.', 1;
GO
