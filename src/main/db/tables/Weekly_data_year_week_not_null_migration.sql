IF EXISTS (
    SELECT 1
    FROM dbo.Weekly_data
    WHERE [Year] IS NULL
)
BEGIN
    DECLARE @NullYearRows BIGINT;
    SELECT @NullYearRows = COUNT_BIG(*)
    FROM dbo.Weekly_data
    WHERE [Year] IS NULL;

    DECLARE @NullYearMessage NVARCHAR(2048) =
        CONCAT('Cannot alter dbo.Weekly_data.Year to NOT NULL because existing NULL values were found. NullYearRows=', @NullYearRows);
    THROW 51030, @NullYearMessage, 1;
END;
GO

IF EXISTS (
    SELECT 1
    FROM dbo.Weekly_data
    WHERE [Week] IS NULL
)
BEGIN
    DECLARE @NullWeekRows BIGINT;
    SELECT @NullWeekRows = COUNT_BIG(*)
    FROM dbo.Weekly_data
    WHERE [Week] IS NULL;

    DECLARE @NullWeekMessage NVARCHAR(2048) =
        CONCAT('Cannot alter dbo.Weekly_data.Week to NOT NULL because existing NULL values were found. NullWeekRows=', @NullWeekRows);
    THROW 51031, @NullWeekMessage, 1;
END;
GO

IF EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'dbo'
      AND TABLE_NAME = 'Weekly_data'
      AND COLUMN_NAME = 'Year'
      AND IS_NULLABLE = 'YES'
)
BEGIN
    ALTER TABLE dbo.Weekly_data ALTER COLUMN [Year] SMALLINT NOT NULL;
END;
GO

IF EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'dbo'
      AND TABLE_NAME = 'Weekly_data'
      AND COLUMN_NAME = 'Week'
      AND IS_NULLABLE = 'YES'
)
BEGIN
    ALTER TABLE dbo.Weekly_data ALTER COLUMN [Week] SMALLINT NOT NULL;
END;
GO
