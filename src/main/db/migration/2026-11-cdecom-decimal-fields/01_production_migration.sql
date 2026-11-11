USE [ReplenishmentDWH];
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.CD_ecom', N'U') IS NULL
        THROW 54101, 'Migration blocked: dbo.CD_ecom does not exist.', 1;
    IF OBJECT_ID(N'dbo.CD_ecom_stage', N'U') IS NULL
        THROW 54102, 'Migration blocked: dbo.CD_ecom_stage does not exist.', 1;

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
        LEFT JOIN sys.columns c ON c.object_id = e.ObjectId AND c.name = e.ColumnName
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
        THROW 54103, 'Migration blocked: every selected column must be BIGINT NOT NULL or DECIMAL(18,2) NOT NULL.', 1;

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
        JOIN sys.stats s ON s.object_id = sc.object_id AND s.stats_id = sc.stats_id
        WHERE TYPE_NAME(c.user_type_id) = N'bigint'
          AND s.auto_created = 0
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
        THROW 54104, 'Migration blocked: a blocking index, user-created statistic, constraint, or schema-bound expression depends on a selected BIGINT column.', 1;

    IF EXISTS (SELECT 1 FROM dbo.CD_ecom WHERE TRY_CONVERT(DECIMAL(18,2), planRub) IS NULL)
        THROW 54105, 'Migration blocked: dbo.CD_ecom.planRub cannot be converted to DECIMAL(18,2).', 1;
    IF EXISTS (SELECT 1 FROM dbo.CD_ecom WHERE TRY_CONVERT(DECIMAL(18,2), stockStoresPcs) IS NULL)
        THROW 54106, 'Migration blocked: dbo.CD_ecom.stockStoresPcs cannot be converted to DECIMAL(18,2).', 1;
    IF EXISTS (SELECT 1 FROM dbo.CD_ecom WHERE TRY_CONVERT(DECIMAL(18,2), stockStoresDdp) IS NULL)
        THROW 54107, 'Migration blocked: dbo.CD_ecom.stockStoresDdp cannot be converted to DECIMAL(18,2).', 1;
    IF EXISTS (SELECT 1 FROM dbo.CD_ecom_stage WHERE TRY_CONVERT(DECIMAL(18,2), planRub) IS NULL)
        THROW 54108, 'Migration blocked: dbo.CD_ecom_stage.planRub cannot be converted to DECIMAL(18,2).', 1;
    IF EXISTS (SELECT 1 FROM dbo.CD_ecom_stage WHERE TRY_CONVERT(DECIMAL(18,2), stockStoresPcs) IS NULL)
        THROW 54109, 'Migration blocked: dbo.CD_ecom_stage.stockStoresPcs cannot be converted to DECIMAL(18,2).', 1;
    IF EXISTS (SELECT 1 FROM dbo.CD_ecom_stage WHERE TRY_CONVERT(DECIMAL(18,2), stockStoresDdp) IS NULL)
        THROW 54110, 'Migration blocked: dbo.CD_ecom_stage.stockStoresDdp cannot be converted to DECIMAL(18,2).', 1;

    DECLARE
        @TargetRowsBefore BIGINT,
        @StageRowsBefore BIGINT,
        @TargetPlanBefore DECIMAL(38,2),
        @TargetPcsBefore DECIMAL(38,2),
        @TargetDdpBefore DECIMAL(38,2),
        @StagePlanBefore DECIMAL(38,2),
        @StagePcsBefore DECIMAL(38,2),
        @StageDdpBefore DECIMAL(38,2);

    SELECT
        @TargetRowsBefore = COUNT_BIG(*),
        @TargetPlanBefore = COALESCE(SUM(CONVERT(DECIMAL(38,2), planRub)), 0),
        @TargetPcsBefore = COALESCE(SUM(CONVERT(DECIMAL(38,2), stockStoresPcs)), 0),
        @TargetDdpBefore = COALESCE(SUM(CONVERT(DECIMAL(38,2), stockStoresDdp)), 0)
    FROM dbo.CD_ecom WITH (HOLDLOCK, TABLOCKX);

    SELECT
        @StageRowsBefore = COUNT_BIG(*),
        @StagePlanBefore = COALESCE(SUM(CONVERT(DECIMAL(38,2), planRub)), 0),
        @StagePcsBefore = COALESCE(SUM(CONVERT(DECIMAL(38,2), stockStoresPcs)), 0),
        @StageDdpBefore = COALESCE(SUM(CONVERT(DECIMAL(38,2), stockStoresDdp)), 0)
    FROM dbo.CD_ecom_stage WITH (HOLDLOCK, TABLOCKX);

    DECLARE
        @StatisticsSchemaName SYSNAME,
        @StatisticsTableName SYSNAME,
        @StatisticsName SYSNAME,
        @DropStatisticsSql NVARCHAR(MAX);

    DECLARE AutoStatisticsCursor CURSOR LOCAL FAST_FORWARD FOR
        SELECT DISTINCT
            OBJECT_SCHEMA_NAME(s.object_id),
            OBJECT_NAME(s.object_id),
            s.name
        FROM @ExpectedColumns e
        JOIN sys.columns c
          ON c.object_id = e.ObjectId
         AND c.name = e.ColumnName
        JOIN sys.stats_columns sc
          ON sc.object_id = c.object_id
         AND sc.column_id = c.column_id
        JOIN sys.stats s
          ON s.object_id = sc.object_id
         AND s.stats_id = sc.stats_id
        WHERE TYPE_NAME(c.user_type_id) = N'bigint'
          AND s.auto_created = 1;

    OPEN AutoStatisticsCursor;
    FETCH NEXT FROM AutoStatisticsCursor
        INTO @StatisticsSchemaName, @StatisticsTableName, @StatisticsName;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @DropStatisticsSql = N'DROP STATISTICS '
                + QUOTENAME(@StatisticsSchemaName) + N'.'
                + QUOTENAME(@StatisticsTableName) + N'.'
                + QUOTENAME(@StatisticsName) + N';';
        EXEC sys.sp_executesql @DropStatisticsSql;

        FETCH NEXT FROM AutoStatisticsCursor
            INTO @StatisticsSchemaName, @StatisticsTableName, @StatisticsName;
    END;

    CLOSE AutoStatisticsCursor;
    DEALLOCATE AutoStatisticsCursor;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id = OBJECT_ID(N'dbo.CD_ecom') AND c.name = N'planRub' AND TYPE_NAME(c.user_type_id) = N'bigint')
        ALTER TABLE dbo.CD_ecom ALTER COLUMN planRub DECIMAL(18,2) NOT NULL;
    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id = OBJECT_ID(N'dbo.CD_ecom') AND c.name = N'stockStoresPcs' AND TYPE_NAME(c.user_type_id) = N'bigint')
        ALTER TABLE dbo.CD_ecom ALTER COLUMN stockStoresPcs DECIMAL(18,2) NOT NULL;
    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id = OBJECT_ID(N'dbo.CD_ecom') AND c.name = N'stockStoresDdp' AND TYPE_NAME(c.user_type_id) = N'bigint')
        ALTER TABLE dbo.CD_ecom ALTER COLUMN stockStoresDdp DECIMAL(18,2) NOT NULL;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND c.name = N'planRub' AND TYPE_NAME(c.user_type_id) = N'bigint')
        ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN planRub DECIMAL(18,2) NOT NULL;
    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND c.name = N'stockStoresPcs' AND TYPE_NAME(c.user_type_id) = N'bigint')
        ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN stockStoresPcs DECIMAL(18,2) NOT NULL;
    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND c.name = N'stockStoresDdp' AND TYPE_NAME(c.user_type_id) = N'bigint')
        ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN stockStoresDdp DECIMAL(18,2) NOT NULL;

    IF EXISTS
    (
        SELECT 1
        FROM @ExpectedColumns e
        JOIN sys.columns c ON c.object_id = e.ObjectId AND c.name = e.ColumnName
        WHERE TYPE_NAME(c.user_type_id) <> N'decimal'
           OR c.precision <> 18
           OR c.scale <> 2
           OR c.is_nullable <> 0
    )
        THROW 54111, 'Migration failed: a selected column did not reach DECIMAL(18,2) NOT NULL.', 1;

    IF @TargetRowsBefore <> (SELECT COUNT_BIG(*) FROM dbo.CD_ecom)
       OR @StageRowsBefore <> (SELECT COUNT_BIG(*) FROM dbo.CD_ecom_stage)
       OR @TargetPlanBefore <> (SELECT COALESCE(SUM(CONVERT(DECIMAL(38,2), planRub)), 0) FROM dbo.CD_ecom)
       OR @TargetPcsBefore <> (SELECT COALESCE(SUM(CONVERT(DECIMAL(38,2), stockStoresPcs)), 0) FROM dbo.CD_ecom)
       OR @TargetDdpBefore <> (SELECT COALESCE(SUM(CONVERT(DECIMAL(38,2), stockStoresDdp)), 0) FROM dbo.CD_ecom)
       OR @StagePlanBefore <> (SELECT COALESCE(SUM(CONVERT(DECIMAL(38,2), planRub)), 0) FROM dbo.CD_ecom_stage)
       OR @StagePcsBefore <> (SELECT COALESCE(SUM(CONVERT(DECIMAL(38,2), stockStoresPcs)), 0) FROM dbo.CD_ecom_stage)
       OR @StageDdpBefore <> (SELECT COALESCE(SUM(CONVERT(DECIMAL(38,2), stockStoresDdp)), 0) FROM dbo.CD_ecom_stage)
        THROW 54112, 'Migration failed: row counts or selected-column control totals changed.', 1;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
