CREATE TABLE dbo.SalesByChannel (
    Id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    LoadSessionId BIGINT NOT NULL,

    seasonYear NVARCHAR(50) NULL,
    season6m NVARCHAR(50) NULL,
    yearMonth NVARCHAR(50) NULL,
    yearSeason NVARCHAR(50) NULL,
    [year] NVARCHAR(50) NOT NULL,
    [month] NVARCHAR(50) NOT NULL,
    salesChannelType NVARCHAR(100) NULL,
    storeRus NVARCHAR(100) NULL,
    typeOfSales NVARCHAR(100) NULL,
    mfpDivision NVARCHAR(100) NULL,
    mfpDepartment NVARCHAR(100) NULL,
    campaignSalesType NVARCHAR(100) NULL,
    seasonality NVARCHAR(50) NULL,
    skuBrandType NVARCHAR(100) NULL,
    salesQuantity INT NOT NULL,
    salesCurr DECIMAL(18,2) NOT NULL,
    gm DECIMAL(18,2) NOT NULL,
    discountTtl DECIMAL(18,2) NOT NULL,
    turnoverCurr DECIMAL(18,2) NOT NULL,
    skuSeasonBudget NVARCHAR(50) NULL,
    storeRusBpo NVARCHAR(100) NULL,
    salesChannelBpo NVARCHAR(100) NULL,
    mfpSubDepartment NVARCHAR(100) NULL,
    skuTm NVARCHAR(100) NULL,
    mfpNode NVARCHAR(100) NULL,
    section NVARCHAR(100) NULL,
    merchandiseSubGroup NVARCHAR(100) NULL,
    skuPhase NVARCHAR(100) NULL,
    skuProductClass NVARCHAR(100) NULL,

    CreatedAt DATETIME2(0) NOT NULL
        CONSTRAINT DF_SalesByChannel_CreatedAt DEFAULT SYSDATETIME(),

    CONSTRAINT FK_SalesByChannel_Load_session
        FOREIGN KEY (LoadSessionId)
            REFERENCES dbo.DWH_Excel_Load_Session(Id)
);
GO

CREATE TABLE dbo.SalesByChannel_raw (
    Id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    LoadSessionId BIGINT NOT NULL,
    ExcelRowNum BIGINT NULL,

    seasonYear NVARCHAR(4000) NULL,
    season6m NVARCHAR(4000) NULL,
    yearMonth NVARCHAR(4000) NULL,
    yearSeason NVARCHAR(4000) NULL,
    [year] NVARCHAR(4000) NULL,
    [month] NVARCHAR(4000) NULL,
    salesChannelType NVARCHAR(4000) NULL,
    storeRus NVARCHAR(4000) NULL,
    typeOfSales NVARCHAR(4000) NULL,
    mfpDivision NVARCHAR(4000) NULL,
    mfpDepartment NVARCHAR(4000) NULL,
    campaignSalesType NVARCHAR(4000) NULL,
    seasonality NVARCHAR(4000) NULL,
    skuBrandType NVARCHAR(4000) NULL,
    salesQuantity NVARCHAR(4000) NULL,
    salesCurr NVARCHAR(4000) NULL,
    gm NVARCHAR(4000) NULL,
    discountTtl NVARCHAR(4000) NULL,
    turnoverCurr NVARCHAR(4000) NULL,
    skuSeasonBudget NVARCHAR(4000) NULL,
    storeRusBpo NVARCHAR(4000) NULL,
    salesChannelBpo NVARCHAR(4000) NULL,
    mfpSubDepartment NVARCHAR(4000) NULL,
    skuTm NVARCHAR(4000) NULL,
    mfpNode NVARCHAR(4000) NULL,
    section NVARCHAR(4000) NULL,
    merchandiseSubGroup NVARCHAR(4000) NULL,
    skuPhase NVARCHAR(4000) NULL,
    skuProductClass NVARCHAR(4000) NULL,

    CreatedAt DATETIME2(0) NOT NULL
        CONSTRAINT DF_SalesByChannel_raw_CreatedAt DEFAULT SYSDATETIME(),

    CONSTRAINT FK_SalesByChannel_raw_Load_session
        FOREIGN KEY (LoadSessionId)
            REFERENCES dbo.DWH_Excel_Load_Session(Id)
);
GO

CREATE TABLE dbo.SalesByChannel_stage (
    LoadSessionId BIGINT NOT NULL,
    ExcelRowNum BIGINT NULL,

    seasonYear NVARCHAR(50) NULL,
    season6m NVARCHAR(50) NULL,
    yearMonth NVARCHAR(50) NULL,
    yearSeason NVARCHAR(50) NULL,
    [year] NVARCHAR(50) NOT NULL,
    [month] NVARCHAR(50) NOT NULL,
    salesChannelType NVARCHAR(100) NULL,
    storeRus NVARCHAR(100) NULL,
    typeOfSales NVARCHAR(100) NULL,
    mfpDivision NVARCHAR(100) NULL,
    mfpDepartment NVARCHAR(100) NULL,
    campaignSalesType NVARCHAR(100) NULL,
    seasonality NVARCHAR(50) NULL,
    skuBrandType NVARCHAR(100) NULL,
    salesQuantity INT NOT NULL,
    salesCurr DECIMAL(18,2) NOT NULL,
    gm DECIMAL(18,2) NOT NULL,
    discountTtl DECIMAL(18,2) NOT NULL,
    turnoverCurr DECIMAL(18,2) NOT NULL,
    skuSeasonBudget NVARCHAR(50) NULL,
    storeRusBpo NVARCHAR(100) NULL,
    salesChannelBpo NVARCHAR(100) NULL,
    mfpSubDepartment NVARCHAR(100) NULL,
    skuTm NVARCHAR(100) NULL,
    mfpNode NVARCHAR(100) NULL,
    section NVARCHAR(100) NULL,
    merchandiseSubGroup NVARCHAR(100) NULL,
    skuPhase NVARCHAR(100) NULL,
    skuProductClass NVARCHAR(100) NULL,

    CONSTRAINT FK_SalesByChannel_stage_Load_session
        FOREIGN KEY (LoadSessionId)
            REFERENCES dbo.DWH_Excel_Load_Session(Id)
);
GO

CREATE INDEX IX_SalesByChannel_LoadSessionId
    ON dbo.SalesByChannel(LoadSessionId);
GO

CREATE INDEX IX_SalesByChannel_raw_LoadSessionId
    ON dbo.SalesByChannel_raw(LoadSessionId);
GO

CREATE INDEX IX_SalesByChannel_stage_LoadSessionId
    ON dbo.SalesByChannel_stage(LoadSessionId);
GO
