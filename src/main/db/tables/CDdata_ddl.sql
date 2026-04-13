USE ReplenishmentDWH;
GO
/* =========================
   1. CD_data_Load_session
   ========================= */

CREATE TABLE dbo.CD_data_Load_session (
                                          Id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                                          FileName NVARCHAR(500) NOT NULL,
                                          FilePath NVARCHAR(1000) NULL,
                                          StartedAt DATETIME2(0) NOT NULL
                                              CONSTRAINT DF_CD_data_Load_session_StartedAt DEFAULT SYSDATETIME(),
                                          FinishedAt DATETIME2(0) NULL,
                                          Status VARCHAR(20) NOT NULL,
                                          TotalRows BIGINT NULL,
                                          LoadedRows BIGINT NULL,
                                          ErrorRows BIGINT NULL,
                                          Message NVARCHAR(2000) NULL
);
GO

/* =========================
   2. CD_data (target)
   ========================= */

CREATE TABLE dbo.CD_data (
                             Id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                             LoadSessionId BIGINT NOT NULL,

    -- Основные данные
                             nazvanie                NVARCHAR(255)        NULL,
                             god                     INT                  NULL,
                             sezon                   INT                  NULL,
                             den                     INT                  NULL,
                             data                    DATE                 NULL,

    -- Организационные признаки
                             sales_channel           NVARCHAR(255)        NULL,
                             store_rus               NVARCHAR(255)        NULL,
                             mfp_division            NVARCHAR(255)        NULL,
                             mfp_department          NVARCHAR(255)        NULL,
                             mfp_sub_department      NVARCHAR(255)        NULL,
                             sku_brand_type          NVARCHAR(255)        NULL,
                             sku_tm                  NVARCHAR(255)        NULL,
                             mfp_node                NVARCHAR(255)        NULL,
                             section                 NVARCHAR(255)        NULL,
                             merchandise_sub_group   NVARCHAR(255)        NULL,
                             campaign_sales          NVARCHAR(255)        NULL,

    -- SKU
                             sku_style_color         INT                  NULL,
                             sku_phase               NVARCHAR(255)        NULL,

    -- Остатки
                             stock_start_pcs         DECIMAL(18,2)        NULL,
                             stock_start_dd          DECIMAL(18,2)        NULL,

    -- Продажи
                             sales_pcs               DECIMAL(18,2)        NULL,
                             sales_rub               DECIMAL(18,2)        NULL,
                             revenue                 DECIMAL(18,2)        NULL,
                             gp                      DECIMAL(18,2)        NULL,
                             cogs                    DECIMAL(18,2)        NULL,

    -- Цены / скидки
                             sales_frp_price         DECIMAL(18,2)        NULL,
                             sales_discount          DECIMAL(18,2)        NULL,

    -- Остатки по магазинам
                             stock_stores_pcs        DECIMAL(18,2)        NULL,
                             stock_stores_dd         DECIMAL(18,2)        NULL,

    -- План
                             plan_rub                INT                  NULL,

    -- Доп. аналитика
                             draivery_cd             NVARCHAR(255)        NULL,
                             sku_color_rus           NVARCHAR(255)        NULL,
                             sku_composition         NVARCHAR(255)        NULL,
                             sku_supplier            NVARCHAR(255)        NULL,
                             sku_name                NVARCHAR(255)        NULL,
                             sku_collection          NVARCHAR(255)        NULL,
                             sku_comment             NVARCHAR(255)        NULL,

    -- Технические поля
                             CreatedAt DATETIME2(0) NOT NULL
                                 CONSTRAINT DF_CD_data_CreatedAt DEFAULT SYSDATETIME(),

                             CONSTRAINT FK_CD_data_Load_session
                                 FOREIGN KEY (LoadSessionId)
                                     REFERENCES dbo.CD_data_Load_session(Id)
);
GO

/* =========================
   3. CD_data_raw (staging)
   ========================= */

CREATE TABLE dbo.CD_data_raw (
                                 Id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                                 LoadSessionId BIGINT NOT NULL,
                                 ExcelRowNum INT NULL,

    -- Основные данные
                                 nazvanie                NVARCHAR(255) NULL,
                                 god                     NVARCHAR(50)  NULL,
                                 sezon                   NVARCHAR(50)  NULL,
                                 den                     NVARCHAR(50)  NULL,
                                 data                    NVARCHAR(50)  NULL,

    -- Организационные признаки
                                 sales_channel           NVARCHAR(255) NULL,
                                 store_rus               NVARCHAR(255) NULL,
                                 mfp_division            NVARCHAR(255) NULL,
                                 mfp_department          NVARCHAR(255) NULL,
                                 mfp_sub_department      NVARCHAR(255) NULL,
                                 sku_brand_type          NVARCHAR(255) NULL,
                                 sku_tm                  NVARCHAR(255) NULL,
                                 mfp_node                NVARCHAR(255) NULL,
                                 section                 NVARCHAR(255) NULL,
                                 merchandise_sub_group   NVARCHAR(255) NULL,
                                 campaign_sales          NVARCHAR(255) NULL,

    -- SKU
                                 sku_style_color         NVARCHAR(50)  NULL,
                                 sku_phase               NVARCHAR(255) NULL,

    -- Остатки
                                 stock_start_pcs         NVARCHAR(50)  NULL,
                                 stock_start_dd          NVARCHAR(50)  NULL,

    -- Продажи / финансы
                                 sales_pcs               NVARCHAR(50)  NULL,
                                 sales_rub               NVARCHAR(50)  NULL,
                                 revenue                 NVARCHAR(50)  NULL,
                                 gp                      NVARCHAR(50)  NULL,
                                 cogs                    NVARCHAR(50)  NULL,

    -- Цены / скидки
                                 sales_frp_price         NVARCHAR(50)  NULL,
                                 sales_discount          NVARCHAR(50)  NULL,

    -- Остатки по магазинам
                                 stock_stores_pcs        NVARCHAR(50)  NULL,
                                 stock_stores_dd         NVARCHAR(50)  NULL,

    -- План
                                 plan_rub                NVARCHAR(50)  NULL,

    -- Доп. аналитика
                                 draivery_cd             NVARCHAR(255) NULL,
                                 sku_color_rus           NVARCHAR(255) NULL,
                                 sku_composition         NVARCHAR(255) NULL,
                                 sku_supplier            NVARCHAR(255) NULL,
                                 sku_name                NVARCHAR(255) NULL,
                                 sku_collection          NVARCHAR(255) NULL,
                                 sku_comment             NVARCHAR(255) NULL,

    -- Технические поля
                                 CreatedAt DATETIME2(0) NOT NULL
                                     CONSTRAINT DF_CD_data_raw_CreatedAt DEFAULT SYSDATETIME(),

                                 CONSTRAINT FK_CD_data_raw_Load_session
                                     FOREIGN KEY (LoadSessionId) REFERENCES dbo.CD_data_Load_session(Id)
);
GO

/* =========================
   4. CD_data_Load_error
   ========================= */

CREATE TABLE dbo.CD_data_Load_error (
                                        Id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,

                                        LoadSessionId BIGINT NOT NULL,

                                        RawId BIGINT NOT NULL,

                                        ErrorAt DATETIME2(0) NOT NULL
                                            CONSTRAINT DF_CD_data_Load_error_ErrorAt DEFAULT SYSDATETIME(),

                                        Stage VARCHAR(50) NOT NULL,

    -- Основное описание ошибки (включая колонку, значение и т.д.)
                                        ErrorMessage NVARCHAR(4000) NOT NULL,

                                        CONSTRAINT FK_CD_data_Load_error_Load_session
                                            FOREIGN KEY (LoadSessionId) REFERENCES dbo.CD_data_Load_session(Id)
);
GO

/* =========================
   5. Индексы
   ========================= */

CREATE INDEX IX_CD_data_LoadSessionId
    ON dbo.CD_data(LoadSessionId);
GO

CREATE INDEX IX_CD_data_raw_LoadSessionId
    ON dbo.CD_data_raw(LoadSessionId);
GO

CREATE INDEX IX_CD_data_Load_error_LoadSessionId
    ON dbo.CD_data_Load_error(LoadSessionId);
GO

CREATE INDEX IX_CD_data_god_sezon
    ON dbo.CD_data(god, sezon);
GO