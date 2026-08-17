IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'OperationType') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session
        ADD OperationType NVARCHAR(30) NOT NULL
            CONSTRAINT DF_DWH_Excel_Load_Session_OperationType DEFAULT ('LOAD') WITH VALUES;
END;
GO

IF EXISTS
(
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.DWH_Excel_Load_Session')
      AND name = N'OperationType'
      AND is_nullable = 1
)
BEGIN
    UPDATE dbo.DWH_Excel_Load_Session
    SET OperationType = N'LOAD'
    WHERE OperationType IS NULL;

    ALTER TABLE dbo.DWH_Excel_Load_Session
        ALTER COLUMN OperationType NVARCHAR(30) NOT NULL;
END;
GO

DECLARE @OperationTypeDefaultName SYSNAME;
DECLARE @OperationTypeDefaultDefinition NVARCHAR(MAX);
DECLARE @DropOperationTypeDefaultSql NVARCHAR(MAX);

SELECT
    @OperationTypeDefaultName = dc.name,
    @OperationTypeDefaultDefinition = dc.definition
FROM sys.default_constraints dc
INNER JOIN sys.columns c
    ON c.object_id = dc.parent_object_id
   AND c.column_id = dc.parent_column_id
WHERE dc.parent_object_id = OBJECT_ID(N'dbo.DWH_Excel_Load_Session')
  AND c.name = N'OperationType';

IF @OperationTypeDefaultName IS NOT NULL
   AND UPPER(REPLACE(@OperationTypeDefaultDefinition, N' ', N'')) <> N'(''LOAD'')'
BEGIN
    SET @DropOperationTypeDefaultSql =
        N'ALTER TABLE dbo.DWH_Excel_Load_Session DROP CONSTRAINT '
        + QUOTENAME(@OperationTypeDefaultName);
    EXEC sys.sp_executesql @DropOperationTypeDefaultSql;
    SET @OperationTypeDefaultName = NULL;
END;

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'OperationType') IS NOT NULL
   AND @OperationTypeDefaultName IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session
        ADD CONSTRAINT DF_DWH_Excel_Load_Session_OperationType
            DEFAULT ('LOAD') FOR OperationType;
END;
GO

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'OperationMode') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD OperationMode NVARCHAR(30) NULL;
END;
GO

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteYear') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD DeleteYear INT NULL;
END;
GO

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteWeek') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD DeleteWeek INT NULL;
END;
GO

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteMonth') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD DeleteMonth INT NULL;
END;
GO

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteYearText') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD DeleteYearText NVARCHAR(50) NULL;
END;
GO

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteMonthText') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD DeleteMonthText NVARCHAR(50) NULL;
END;
GO

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'SourceLoadSessionId') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD SourceLoadSessionId BIGINT NULL;
END;
GO

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteCriterion') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD DeleteCriterion NVARCHAR(50) NULL;
END;
GO

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteParameter1Name') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD DeleteParameter1Name NVARCHAR(50) NULL;
END;
GO

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteParameter1Value') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD DeleteParameter1Value NVARCHAR(1000) NULL;
END;
GO

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteParameter2Name') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD DeleteParameter2Name NVARCHAR(50) NULL;
END;
GO

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteParameter2Value') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD DeleteParameter2Value NVARCHAR(1000) NULL;
END;
GO

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeletedRows') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session ADD DeletedRows BIGINT NULL;
END;
GO
