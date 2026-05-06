
CREATE TABLE dbo.Weekly_data (
                                 Id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                                 LoadSessionId BIGINT NOT NULL,

    -- Периоды
                                 Year21 SMALLINT NULL,
                                 Week21 SMALLINT NULL,
                                 YearCorr SMALLINT NULL,
                                 WeekCorr SMALLINT NULL,
                                 Year SMALLINT NULL,
                                 Week SMALLINT NULL,

    -- Организационные признаки
                                 SalesChannelBpo NVARCHAR(255) NULL,
                                 StoreRusBpo NVARCHAR(255) NULL,
                                 StoreRus NVARCHAR(255) NULL,
                                 MfpDivisionNew NVARCHAR(255) NULL,
                                 MfpDepartment NVARCHAR(255) NULL,
                                 SkuSeasonBudget NVARCHAR(255) NULL,
                                 TypeOfSales NVARCHAR(255) NULL,

    -- Остатки
                                 TotalStockPcs DECIMAL(18,2),
                                 TotalStockDdp DECIMAL(18,2),

    -- Продажи
                                 SalesPcs DECIMAL(18,2),
                                 SalesRub DECIMAL(18,2),

    -- Финансы
                                 Revenue DECIMAL(18,2),
                                 Gp DECIMAL(18,2),
                                 DiscountTotalRub DECIMAL(18,2),

    -- Доп. аналитика
                                 MfpDivision NVARCHAR(255) NULL,
                                 Season NVARCHAR(255) NULL,
                                 Month NVARCHAR(255) NULL,
                                 Bundle NVARCHAR(255) NULL,
                                 Seasonality NVARCHAR(255) NULL,

    -- Технические поля
                                 CreatedAt DATETIME2(0) NOT NULL
                                     CONSTRAINT DF_Weekly_data_CreatedAt DEFAULT SYSDATETIME(),

                                 CONSTRAINT FK_Weekly_data_Load_session
                                     FOREIGN KEY (LoadSessionId) REFERENCES dbo.DWH_Excel_Load_Session(Id)
);
GO

CREATE TABLE dbo.Weekly_data_raw (
                                     Id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                                     LoadSessionId BIGINT NOT NULL,
                                     ExcelRowNum BIGINT NULL,

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
                                     SkuSeasonBudget NVARCHAR(255) NULL,
                                     TypeOfSales NVARCHAR(255) NULL,

    -- Остатки
                                     TotalStockPcs NVARCHAR(255) NULL,
                                     TotalStockDdp NVARCHAR(255) NULL,

    -- Продажи
                                     SalesPcs NVARCHAR(255) NULL,
                                     SalesRub NVARCHAR(255) NULL,

    -- Финансы
                                     Revenue NVARCHAR(255) NULL,
                                     Gp NVARCHAR(255) NULL,
                                     DiscountTotalRub NVARCHAR(255) NULL,

    -- Доп. аналитика
                                     MfpDivision NVARCHAR(255) NULL,
                                     Season NVARCHAR(255) NULL,
                                     Month NVARCHAR(255) NULL,
                                     Bundle NVARCHAR(255) NULL,
                                     Seasonality NVARCHAR(255) NULL,

    -- Технические поля
                                     CreatedAt DATETIME2(0) NOT NULL
                                         CONSTRAINT DF_Weekly_data_raw_CreatedAt DEFAULT SYSDATETIME(),

                                     CONSTRAINT FK_Weekly_data_raw_Load_session
                                         FOREIGN KEY (LoadSessionId) REFERENCES dbo.DWH_Excel_Load_Session(Id)
);
GO

CREATE INDEX IX_Weekly_data_LoadSessionId
    ON dbo.Weekly_data(LoadSessionId);
GO

CREATE INDEX IX_Weekly_data_raw_LoadSessionId
    ON dbo.Weekly_data_raw(LoadSessionId);
GO

CREATE INDEX IX_Weekly_data_Year_Week
    ON dbo.Weekly_data(Year, Week);
GO