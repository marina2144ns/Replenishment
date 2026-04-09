USE ReplenishmentDWH;
GO

/* =========================
   1. Weekly_data_Load_session
   ========================= */

CREATE TABLE dbo.Weekly_data_Load_session (
                                              Id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                                              FileName NVARCHAR(500) NOT NULL,
                                              FilePath NVARCHAR(1000) NULL,
                                              StartedAt DATETIME2(0) NOT NULL
                                                  CONSTRAINT DF_Weekly_data_Load_session_StartedAt DEFAULT SYSDATETIME(),
                                              FinishedAt DATETIME2(0) NULL,
                                              Status VARCHAR(20) NOT NULL,
                                              TotalRows BIGINT NULL,
                                              LoadedRows BIGINT NULL,
                                              ErrorRows BIGINT NULL,
                                              Message NVARCHAR(2000) NULL
);
GO

/* =========================
   2. Weekly_data (target)
   ========================= */

CREATE TABLE dbo.Weekly_data (
                                 Id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                                 LoadSessionId BIGINT NOT NULL,

    -- Периоды
                                 Year21 SMALLINT NULL,
                                 Week21 SMALLINT NULL,
                                 YearCorr SMALLINT NULL,
                                 WeekCorr SMALLINT NULL,
                                 Year SMALLINT NOT NULL,
                                 Week SMALLINT NOT NULL,

    -- Организационные признаки
                                 SalesChannelBpo VARCHAR(50) NULL,
                                 StoreRusBpo VARCHAR(50) NULL,
                                 StoreRus VARCHAR(50) NULL,
                                 MfpDivisionNew VARCHAR(50) NULL,
                                 MfpDepartment VARCHAR(50) NULL,
                                 SkuSeasonBudget VARCHAR(20) NULL,
                                 TypeOfSales VARCHAR(20) NULL,

    -- Остатки
                                 TotalStockPcs INT,
                                 TotalStockDdp DECIMAL(18,2),

    -- Продажи
                                 SalesPcs INT,
                                 SalesRub DECIMAL(18,2),

    -- Финансы
                                 Revenue DECIMAL(18,2),
                                 Gp DECIMAL(18,2),
                                 DiscountTotalRub DECIMAL(18,2),

    -- Доп. аналитика
                                 MfpDivision VARCHAR(50) NULL,
                                 Season VARCHAR(20) NULL,
                                 Month VARCHAR(20) NULL,
                                 Bundle VARCHAR(20) NULL,
                                 Seasonality VARCHAR(20) NULL,

    -- Технические поля
                                 CreatedAt DATETIME2(0) NOT NULL
                                     CONSTRAINT DF_Weekly_data_CreatedAt DEFAULT SYSDATETIME(),

                                 CONSTRAINT FK_Weekly_data_Load_session
                                     FOREIGN KEY (LoadSessionId) REFERENCES dbo.Weekly_data_Load_session(Id)
);
GO

/* =========================
   3. Weekly_data_raw (staging)
   ========================= */

CREATE TABLE dbo.Weekly_data_raw (
                                     Id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                                     LoadSessionId BIGINT NOT NULL,

    -- Периоды
                                     Year21 NVARCHAR(50) NULL,
                                     Week21 NVARCHAR(50) NULL,
                                     YearCorr NVARCHAR(50) NULL,
                                     WeekCorr NVARCHAR(50) NULL,
                                     Year NVARCHAR(50) NULL,
                                     Week NVARCHAR(50) NULL,

    -- Организационные признаки
                                     SalesChannelBpo NVARCHAR(255) NULL,
                                     StoreRusBpo NVARCHAR(255) NULL,
                                     StoreRus NVARCHAR(255) NULL,
                                     MfpDivisionNew NVARCHAR(255) NULL,
                                     MfpDepartment NVARCHAR(255) NULL,
                                     SkuSeasonBudget NVARCHAR(100) NULL,
                                     TypeOfSales NVARCHAR(100) NULL,

    -- Остатки
                                     TotalStockPcs NVARCHAR(50) NULL,
                                     TotalStockDdp NVARCHAR(50) NULL,

    -- Продажи
                                     SalesPcs NVARCHAR(50) NULL,
                                     SalesRub NVARCHAR(50) NULL,

    -- Финансы
                                     Revenue NVARCHAR(50) NULL,
                                     Gp NVARCHAR(50) NULL,
                                     DiscountTotalRub NVARCHAR(50) NULL,

    -- Доп. аналитика
                                     MfpDivision NVARCHAR(255) NULL,
                                     Season NVARCHAR(100) NULL,
                                     Month NVARCHAR(100) NULL,
                                     Bundle NVARCHAR(100) NULL,
                                     Seasonality NVARCHAR(100) NULL,

    -- Технические поля
                                     CreatedAt DATETIME2(0) NOT NULL
                                         CONSTRAINT DF_Weekly_data_raw_CreatedAt DEFAULT SYSDATETIME(),

                                     CONSTRAINT FK_Weekly_data_raw_Load_session
                                         FOREIGN KEY (LoadSessionId) REFERENCES dbo.Weekly_data_Load_session(Id)
);
GO

/* =========================
   4. Weekly_data_Load_error
   ========================= */

CREATE TABLE dbo.Weekly_data_Load_error (
                                            Id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,

                                            LoadSessionId BIGINT NOT NULL,

                                            RawId BIGINT NOT NULL,

                                            ErrorAt DATETIME2(0) NOT NULL
                                                CONSTRAINT DF_Weekly_data_Load_error_ErrorAt DEFAULT SYSDATETIME(),

                                            Stage VARCHAR(50) NOT NULL,

    -- Основное описание ошибки (включая колонку, значение и т.д.)
                                            ErrorMessage NVARCHAR(4000) NOT NULL,


                                            CONSTRAINT FK_Weekly_data_Load_error_Load_session
                                                FOREIGN KEY (LoadSessionId) REFERENCES dbo.Weekly_data_Load_session(Id)

);


/* =========================
   5. Индексы
   ========================= */

CREATE INDEX IX_Weekly_data_LoadSessionId
    ON dbo.Weekly_data(LoadSessionId);
GO

CREATE INDEX IX_Weekly_data_raw_LoadSessionId
    ON dbo.Weekly_data_raw(LoadSessionId);
GO

CREATE INDEX IX_Weekly_data_Load_error_LoadSessionId
    ON dbo.Weekly_data_Load_error(LoadSessionId);
GO

CREATE INDEX IX_Weekly_data_Year_Week
    ON dbo.Weekly_data(Year, Week);
GO