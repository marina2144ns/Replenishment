IF EXISTS (
    SELECT 1
    FROM dbo.CD_ecom
    WHERE [year] IS NULL
       OR season IS NULL
       OR [day] IS NULL
       OR name IS NULL
       OR LTRIM(RTRIM(REPLACE(REPLACE(name, NCHAR(160), N' '), NCHAR(8239), N' '))) = N''
)
OR EXISTS (
    SELECT 1
    FROM dbo.CD_ecom_stage
    WHERE [year] IS NULL
       OR season IS NULL
       OR [day] IS NULL
       OR name IS NULL
       OR LTRIM(RTRIM(REPLACE(REPLACE(name, NCHAR(160), N' '), NCHAR(8239), N' '))) = N''
)
BEGIN
    ;THROW 50001,
        'CDEcom schema cannot make year, season, name and day NOT NULL while legacy target or stage rows violate the required-field contract.',
        1;
END;
GO

IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'name' AND is_nullable = 1
)
    ALTER TABLE dbo.CD_ecom ALTER COLUMN name NVARCHAR(255) NOT NULL;
GO
IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'year' AND is_nullable = 1
)
    ALTER TABLE dbo.CD_ecom ALTER COLUMN [year] INT NOT NULL;
GO
IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'season' AND is_nullable = 1
)
    ALTER TABLE dbo.CD_ecom ALTER COLUMN season INT NOT NULL;
GO
IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'day' AND is_nullable = 1
)
    ALTER TABLE dbo.CD_ecom ALTER COLUMN [day] INT NOT NULL;
GO

IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'name' AND is_nullable = 1
)
    ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN name NVARCHAR(255) NOT NULL;
GO
IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'year' AND is_nullable = 1
)
    ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN [year] INT NOT NULL;
GO
IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'season' AND is_nullable = 1
)
    ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN season INT NOT NULL;
GO
IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'day' AND is_nullable = 1
)
    ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN [day] INT NOT NULL;
GO
