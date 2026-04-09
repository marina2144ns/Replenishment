USE ReplenishmentDWH;
GO


IF OBJECT_ID('dbo.ABCData_STG','U')      IS NOT NULL DROP TABLE dbo.ABCData_STG;
IF OBJECT_ID('dbo.BulkLoadErrors','U')   IS NOT NULL DROP TABLE dbo.BulkLoadErrors;
IF OBJECT_ID('dbo.StoreTurnover','U')    IS NOT NULL DROP TABLE dbo.StoreTurnover;
IF OBJECT_ID('dbo.ABCData','U')          IS NOT NULL DROP TABLE dbo.ABCData;
GO


CREATE TABLE StoreTurnover(
                              Id BIGINT IDENTITY(1,1) PRIMARY KEY,
                              SKU VARCHAR (50),
                              Period DATE,
                              StoreRus VARCHAR(50) ,
                              RemainingSum INT,
                              RemainingDays INT ,
                              SalesQuantity INT ,
                              Sales INT,
                              ASP INT ,
                              Revenue INT ,
                              GP  INT,
                              DiscountTotal INT,
                              LoadDateTime DATETIME DEFAULT GETDATE()
);




CREATE TABLE BulkLoadErrors (
                                ErrorID BIGINT IDENTITY(1,1) PRIMARY KEY,
                                ErrorDescription NVARCHAR(MAX),
                                LoadDateTime DATETIME DEFAULT GETDATE()
);


CREATE TABLE ABCData (
                         Id BIGINT IDENTITY(1,1) PRIMARY KEY,
                         SkuTM VARCHAR(150),                    -- торговая марка
                         Section VARCHAR(150),                -- раздел
                         MFPDepartment VARCHAR(150),          -- отдел
                         MerchandiseSubGroup VARCHAR(150),    -- подгруппа
                         SKUItem VARCHAR(50),                 -- артикул
                         SalesCurr INT,                       -- текущие продажи
                         AccumPercent INT,            -- % накопления (оставим как текст, т.к. может быть с символом '%')
                         ABC CHAR(1),
                         ABCNO3_Units CHAR(1),
                         ABCNO3_Rev CHAR(1),
                         ABCNO6_Units CHAR(1),
                         ABCNO6_Rev CHAR(1),
                         ABCNO12_Units CHAR(1),
                         ABCNO12_Rev CHAR(1),
                         SupplyDate DATE,                     -- дата поставки
                         LoadDateTime DATETIME DEFAULT GETDATE()
);

/* =========================
   1) Целевая таблица ABCData
   ========================= */
CREATE TABLE dbo.ABCData
(
    Id                   BIGINT IDENTITY(1,1) PRIMARY KEY,
    SkuTM                NVARCHAR(150)      NULL,      -- торговая марка
    Section              NVARCHAR(150)      NULL,      -- раздел
    MFPDepartment        NVARCHAR(150)      NULL,      -- отдел
    MerchandiseSubGroup  NVARCHAR(150)      NULL,      -- подгруппа
    SKUItem              NVARCHAR(50)       NOT NULL,  -- артикул (бизнес-ключ)
    SalesCurr            INT                NULL,      -- текущие продажи (число)
    AccumPercent         INT                NULL,      -- накопленный процент (уже как INT)
    ABC                  NCHAR(1)           NULL,      -- A/B/C
    ABCNO3_Units         NCHAR(1)           NULL,
    ABCNO3_Rev           NCHAR(1)           NULL,
    ABCNO6_Units         NCHAR(1)           NULL,
    ABCNO6_Rev           NCHAR(1)           NULL,
    ABCNO12_Units        NCHAR(1)           NULL,
    ABCNO12_Rev          NCHAR(1)           NULL,
    SupplyDate           DATE               NULL,      -- дата поставки
    LoadDateTime         DATETIME2(0)       NOT NULL CONSTRAINT DF_ABCData_LoadDateTime DEFAULT SYSUTCDATETIME()
);
GO

/* Уникальный индекс по бизнес-ключу для быстрого MERGE */
CREATE UNIQUE INDEX UX_ABCData_SkuItem ON dbo.ABCData (SKUItem);
GO


/* =========================================
   2) STAGING-таблица для BulkCopy из CSV (heap)
   ========================================= */
CREATE TABLE dbo.ABCData_STG
(
    SkuTM                NVARCHAR(150) NULL,
    Section              NVARCHAR(150) NULL,
    MFPDepartment        NVARCHAR(150) NULL,
    MerchandiseSubGroup  NVARCHAR(150) NULL,
    SKUItem              NVARCHAR(50)  NOT NULL,
    SalesCurrRaw         NVARCHAR(50)  NULL,   -- сырое значение (текст), в MERGE приводим к INT
    AccumPercentRaw      NVARCHAR(50)  NULL,   -- сырое значение (текст), в MERGE приводим к INT
    ABC                  NCHAR(1)      NULL,
    ABCNO                NCHAR(1)      NULL,   -- в MERGE раскладываем по нужному столбцу в зависимости от "month"
    SupplyDateRaw        NVARCHAR(20)  NULL    -- формат dd.MM.yyyy, в MERGE конвертируем в DATE
);
GO
-- Внимание: индексы и триггеры на STG не создаём — нужна максимально быстрая загрузка.




/* Процедура: перенос из STG в ABCData (обновление/вставка), с раскладкой ABCNO */
IF OBJECT_ID('dbo.usp_ABCData_Merge','P') IS NOT NULL DROP PROCEDURE dbo.usp_ABCData_Merge;
GO
CREATE PROCEDURE dbo.usp_ABCData_Merge
    @timePeriod NVARCHAR(4)  -- '3U','3R','6U','6R','12U','12R'
AS
BEGIN
    SET NOCOUNT ON;

    /* Если в STG случайно есть дубликаты по SKUItem — берём последнюю строку (MAX по SupplyDate, затем по SalesCurr, затем "последняя по порядку") */
WITH S0 AS (
    SELECT
        s.SkuTM,
        s.Section,
        s.MFPDepartment,
        s.MerchandiseSubGroup,
        s.SKUItem,
        TRY_CONVERT(int, s.SalesCurrRaw)     AS SalesCurr,
        TRY_CONVERT(int, s.AccumPercentRaw)  AS AccumPercent,
        s.ABC,
        s.ABCNO,
        TRY_CONVERT(date, s.SupplyDateRaw, 104)               AS SupplyDate,
        ROW_NUMBER() OVER (
                PARTITION BY s.SKUItem
                ORDER BY
                    TRY_CONVERT(date, s.SupplyDateRaw, 104) DESC,
                    TRY_CONVERT(int, REPLACE(s.SalesCurrRaw, ' ', '')) DESC
            ) AS rn
    FROM dbo.ABCData_STG s
),
     S AS (
         SELECT
             SkuTM, Section, MFPDepartment, MerchandiseSubGroup, SKUItem,
    SalesCurr, AccumPercent, ABC,
    CASE WHEN @timePeriod = '3U'  THEN ABCNO END AS abcno3_Units,
            CASE WHEN @timePeriod = '3R'  THEN ABCNO END AS abcno3_Rev,
            CASE WHEN @timePeriod = '6U'  THEN ABCNO END AS abcno6_Units,
            CASE WHEN @timePeriod = '6R'  THEN ABCNO END AS abcno6_Rev,
            CASE WHEN @timePeriod = '12U' THEN ABCNO END AS abcno12_Units,
            CASE WHEN @timePeriod = '12R' THEN ABCNO END AS abcno12_Rev,
            SupplyDate
        FROM S0
        WHERE rn = 1
    )
    MERGE dbo.ABCData AS T
    USING S
      ON T.SKUItem = S.SKUItem
    WHEN MATCHED THEN UPDATE SET
                                                                                                                                                                                                                                                                                                                                                T.SkuTM               = S.SkuTM,
                                                                                                                                                                                                                                                                                                                                                T.Section             = S.Section,
                                                                                                                                                                                                                                                                                                                                                T.MFPDepartment       = S.MFPDepartment,
                                                                                                                                                                                                                                                                                                                                                T.MerchandiseSubGroup = S.MerchandiseSubGroup,
                                                                                                                                                                                                                                                                                                                                                T.SalesCurr           = S.SalesCurr,
                                                                                                                                                                                                                                                                                                                                                T.AccumPercent        = S.AccumPercent,
                                                                                                                                                                                                                                                                                                                                                T.ABC                 = S.ABC,
                                                                                                                                                                                                                                                                                                                                                T.abcno3_Units        = COALESCE(S.abcno3_Units,  T.abcno3_Units),
                                                                                                                                                                                                                                                                                                                                                T.abcno3_Rev          = COALESCE(S.abcno3_Rev,    T.abcno3_Rev),
                                                                                                                                                                                                                                                                                                                                                T.abcno6_Units        = COALESCE(S.abcno6_Units,  T.abcno6_Units),
                                                                                                                                                                                                                                                                                                                                                T.abcno6_Rev          = COALESCE(S.abcno6_Rev,    T.abcno6_Rev),
                                                                                                                                                                                                                                                                                                                                                T.abcno12_Units       = COALESCE(S.abcno12_Units, T.abcno12_Units),
                                                                                                                                                                                                                                                                                                                                                T.abcno12_Rev         = COALESCE(S.abcno12_Rev,   T.abcno12_Rev),
                                                                                                                                                                                                                                                                                                                                                T.SupplyDate          = S.SupplyDate
                                                                                                                                                                                                                                                                                                                                                WHEN NOT MATCHED BY TARGET THEN INSERT
                      (SkuTM, Section, MFPDepartment, MerchandiseSubGroup, SKUItem,
                      SalesCurr, AccumPercent, ABC,
                      abcno3_Units, abcno3_Rev, abcno6_Units, abcno6_Rev, abcno12_Units, abcno12_Rev,
                      SupplyDate)
                      VALUES
                                                                                                                                                                                                                                                                                                                                                (S.SkuTM, S.Section, S.MFPDepartment, S.MerchandiseSubGroup, S.SKUItem,
                                                                                                                                                                                                                                                                                                                                                S.SalesCurr, S.AccumPercent, S.ABC,
                                                                                                                                                                                                                                                                                                                                                S.abcno3_Units, S.abcno3_Rev, S.abcno6_Units, S.abcno6_Rev, S.abcno12_Units, S.abcno12_Rev,
                                                                                                                                                                                                                                                                                                                                                S.SupplyDate);
END
GO




GRANT SELECT, INSERT, UPDATE, DELETE ON dbo.ABCData_STG TO ReplenishmentREAD;
GRANT SELECT, INSERT, UPDATE, DELETE ON dbo.ABCData     TO ReplenishmentREAD;

-- Для TRUNCATE нужно ALTER на таблицу или быть db_owner.
GRANT ALTER ON OBJECT::dbo.ABCData_STG TO ReplenishmentREAD;

-- Для выполнения процедуры MERGE (если используешь usp_ABCData_Merge):
GRANT EXECUTE ON dbo.usp_ABCData_Merge TO ReplenishmentREAD;
