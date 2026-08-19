USE [ReplenishmentDWH];
GO

SET XACT_ABORT ON;
GO

/*
   CDEcom required delete fields migration.

   Contract:
   - dbo.CD_ecom:       name, year, season, day -> NOT NULL
   - dbo.CD_ecom_stage: name, year, season, day -> NOT NULL

   The migration is atomic: if legacy data violates the contract or any ALTER
   fails, all schema/index changes are rolled back.

   Dependent canonical indexes:
   - IX_CD_ecom_year_season ON dbo.CD_ecom([year], season)
   - IX_CD_ecom_name_day    ON dbo.CD_ecom(name, [day])
*/

BEGIN TRY
    BEGIN TRANSACTION;

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
        THROW 50001,
            'CDEcom schema cannot make year, season, name and day NOT NULL while legacy target or stage rows violate the required-field contract.',
            1;
    END;

    /* Drop dependent indexes only when one of their columns still needs ALTER. */
    IF EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE object_id = OBJECT_ID(N'dbo.CD_ecom')
          AND name = N'IX_CD_ecom_year_season'
    )
    AND EXISTS (
        SELECT 1
        FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.CD_ecom')
          AND name IN (N'year', N'season')
          AND is_nullable = 1
    )
    BEGIN
        DROP INDEX IX_CD_ecom_year_season ON dbo.CD_ecom;
    END;

    IF EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE object_id = OBJECT_ID(N'dbo.CD_ecom')
          AND name = N'IX_CD_ecom_name_day'
    )
    AND EXISTS (
        SELECT 1
        FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.CD_ecom')
          AND name IN (N'name', N'day')
          AND is_nullable = 1
    )
    BEGIN
        DROP INDEX IX_CD_ecom_name_day ON dbo.CD_ecom;
    END;

    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'name' AND is_nullable = 1
    )
        ALTER TABLE dbo.CD_ecom ALTER COLUMN name NVARCHAR(255) NOT NULL;

    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'year' AND is_nullable = 1
    )
        ALTER TABLE dbo.CD_ecom ALTER COLUMN [year] INT NOT NULL;

    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'season' AND is_nullable = 1
    )
        ALTER TABLE dbo.CD_ecom ALTER COLUMN season INT NOT NULL;

    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'day' AND is_nullable = 1
    )
        ALTER TABLE dbo.CD_ecom ALTER COLUMN [day] INT NOT NULL;

    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'name' AND is_nullable = 1
    )
        ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN name NVARCHAR(255) NOT NULL;

    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'year' AND is_nullable = 1
    )
        ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN [year] INT NOT NULL;

    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'season' AND is_nullable = 1
    )
        ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN season INT NOT NULL;

    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'day' AND is_nullable = 1
    )
        ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN [day] INT NOT NULL;

    /* Restore canonical indexes if absent. */
    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE object_id = OBJECT_ID(N'dbo.CD_ecom')
          AND name = N'IX_CD_ecom_year_season'
    )
    BEGIN
        CREATE NONCLUSTERED INDEX IX_CD_ecom_year_season
            ON dbo.CD_ecom([year], season);
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE object_id = OBJECT_ID(N'dbo.CD_ecom')
          AND name = N'IX_CD_ecom_name_day'
    )
    BEGIN
        CREATE NONCLUSTERED INDEX IX_CD_ecom_name_day
            ON dbo.CD_ecom(name, [day]);
    END;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;

    THROW;
END CATCH;
GO
