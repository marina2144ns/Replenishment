IF EXISTS (
    SELECT 1
    FROM dbo.CD_data
    WHERE god IS NULL
       OR sezon IS NULL
       OR den IS NULL
       OR nazvanie IS NULL
       OR LTRIM(RTRIM(REPLACE(REPLACE(nazvanie, NCHAR(160), N' '), NCHAR(8239), N' '))) = N''
)
OR EXISTS (
    SELECT 1
    FROM dbo.CD_data_stage
    WHERE god IS NULL
       OR sezon IS NULL
       OR den IS NULL
       OR nazvanie IS NULL
       OR LTRIM(RTRIM(REPLACE(REPLACE(nazvanie, NCHAR(160), N' '), NCHAR(8239), N' '))) = N''
)
BEGIN
    ;THROW 50001,
        'CDData schema cannot make god, sezon, nazvanie and den NOT NULL while legacy target or stage rows violate the required-field contract.',
        1;
END;
GO

IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'nazvanie' AND is_nullable = 1
)
    ALTER TABLE dbo.CD_data ALTER COLUMN nazvanie NVARCHAR(255) NOT NULL;
GO
IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'god' AND is_nullable = 1
)
    ALTER TABLE dbo.CD_data ALTER COLUMN god INT NOT NULL;
GO
IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'sezon' AND is_nullable = 1
)
    ALTER TABLE dbo.CD_data ALTER COLUMN sezon INT NOT NULL;
GO
IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'den' AND is_nullable = 1
)
    ALTER TABLE dbo.CD_data ALTER COLUMN den INT NOT NULL;
GO

IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.CD_data_stage') AND name = N'nazvanie' AND is_nullable = 1
)
    ALTER TABLE dbo.CD_data_stage ALTER COLUMN nazvanie NVARCHAR(255) NOT NULL;
GO
IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.CD_data_stage') AND name = N'god' AND is_nullable = 1
)
    ALTER TABLE dbo.CD_data_stage ALTER COLUMN god INT NOT NULL;
GO
IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.CD_data_stage') AND name = N'sezon' AND is_nullable = 1
)
    ALTER TABLE dbo.CD_data_stage ALTER COLUMN sezon INT NOT NULL;
GO
IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.CD_data_stage') AND name = N'den' AND is_nullable = 1
)
    ALTER TABLE dbo.CD_data_stage ALTER COLUMN den INT NOT NULL;
GO
