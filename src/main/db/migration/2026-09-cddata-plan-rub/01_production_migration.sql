USE [ReplenishmentDWH];
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.CD_data', N'U') IS NULL
        THROW 52011, 'Migration blocked: dbo.CD_data does not exist.', 1;

    IF OBJECT_ID(N'dbo.CD_data_stage', N'U') IS NULL
        THROW 52012, 'Migration blocked: dbo.CD_data_stage does not exist.', 1;

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
        THROW 52013, 'Migration blocked: dbo.CD_data.plan_rub must be INT NOT NULL or DECIMAL(18,2) NOT NULL.', 1;

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
        THROW 52014, 'Migration blocked: dbo.CD_data_stage.plan_rub must be INT NOT NULL or DECIMAL(18,2) NOT NULL.', 1;

    IF EXISTS (
        SELECT 1
        FROM sys.columns c
        WHERE c.object_id = OBJECT_ID(N'dbo.CD_data')
          AND c.name = N'plan_rub'
          AND TYPE_NAME(c.user_type_id) = N'int'
          AND c.is_nullable = 0
    )
        ALTER TABLE dbo.CD_data ALTER COLUMN plan_rub DECIMAL(18,2) NOT NULL;

    IF EXISTS (
        SELECT 1
        FROM sys.columns c
        WHERE c.object_id = OBJECT_ID(N'dbo.CD_data_stage')
          AND c.name = N'plan_rub'
          AND TYPE_NAME(c.user_type_id) = N'int'
          AND c.is_nullable = 0
    )
        ALTER TABLE dbo.CD_data_stage ALTER COLUMN plan_rub DECIMAL(18,2) NOT NULL;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
