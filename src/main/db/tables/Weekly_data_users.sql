USE master;
GO

/* 1. Создать серверный login */
IF NOT EXISTS (
        SELECT 1
        FROM sys.server_principals
        WHERE name = N'replenishment_reader'
    )
    BEGIN
        CREATE LOGIN [replenishment_reader]
            WITH PASSWORD = 'replUser2026';
    END
GO

/* 2. Перейти в нужную БД */
USE ReplenishmentDWH;
GO

/* 3. Создать пользователя в базе */
IF NOT EXISTS (
        SELECT 1
        FROM sys.database_principals
        WHERE name = N'replenishment_reader'
    )
    BEGIN
        CREATE USER [replenishment_reader]
            FOR LOGIN [replenishment_reader];
    END
GO

/* 4. Выдать права только на чтение нужных таблиц */
GRANT SELECT ON dbo.Weekly_data TO [replenishment_reader];
GRANT SELECT ON dbo.Weekly_data_raw TO [replenishment_reader];
GRANT SELECT ON dbo.Weekly_data_Load_session TO [replenishment_reader];
GRANT SELECT ON dbo.Weekly_data_Load_error TO [replenishment_reader];
GO