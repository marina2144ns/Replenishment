USE ReplenishmentDWH;
GO

/*==============================================================*/
/* 1. Raw-таблица                                               */
/*    Сюда грузим как есть, почти всё строками                  */
/*==============================================================*/
CREATE TABLE dbo.CD_ecom_raw
(
    Id                          BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    LoadSessionId               BIGINT                NOT NULL,
    ExcelRowNum                 BIGINT                NULL,

    name                        NVARCHAR(4000)        NULL,
    [year]                      NVARCHAR(50)          NULL,
    season                      NVARCHAR(50)          NULL,
    [day]                       NVARCHAR(50)          NULL,
    [data]                      NVARCHAR(50)          NULL,
    salesChannelBpo             NVARCHAR(4000)        NULL,
    storeRus                    NVARCHAR(4000)        NULL,
    mfpDivision                 NVARCHAR(4000)        NULL,
    mfpDepartment               NVARCHAR(4000)        NULL,
    mfpSubDepartment            NVARCHAR(4000)        NULL,
    skuBrandType                NVARCHAR(4000)        NULL,
    skuTm                       NVARCHAR(4000)        NULL,
    mfpNode                     NVARCHAR(4000)        NULL,
    section                     NVARCHAR(4000)        NULL,
    merchandiseSubGroup         NVARCHAR(4000)        NULL,
    campaignSalesType           NVARCHAR(4000)        NULL,
    skuStyleColor               NVARCHAR(100)         NULL,
    skuPhase                    NVARCHAR(4000)        NULL,
    orderPcs                    NVARCHAR(100)         NULL,
    orderRub                    NVARCHAR(100)         NULL,
    foundPcs                    NVARCHAR(100)         NULL,
    foundRub                    NVARCHAR(100)         NULL,
    salesPcs                    NVARCHAR(100)         NULL,
    salesRub                    NVARCHAR(100)         NULL,
    revenue                     NVARCHAR(100)         NULL,
    gp                          NVARCHAR(100)         NULL,
    cogs                        NVARCHAR(100)         NULL,
    salesDiscount               NVARCHAR(100)         NULL,
    planRub                     NVARCHAR(100)         NULL,
    stockStoresPcs              NVARCHAR(100)         NULL,
    stockStoresDdp              NVARCHAR(100)         NULL,
    cdDrivers                   NVARCHAR(4000)        NULL,
    skuSupplierModel            NVARCHAR(4000)        NULL,
    skuComposition              NVARCHAR(4000)        NULL,
    skuColorRussian             NVARCHAR(4000)        NULL,
    skuName                     NVARCHAR(4000)        NULL,
    skuCommentBuyer             NVARCHAR(4000)        NULL,
    skuCollection               NVARCHAR(4000)        NULL,

    CreatedAt                   DATETIME2             NOT NULL CONSTRAINT DF_CD_ecom_raw_CreatedAt DEFAULT SYSUTCDATETIME(),

    CONSTRAINT FK_CD_ecom_raw_LoadSession
        FOREIGN KEY (LoadSessionId) REFERENCES dbo.DWH_Excel_Load_Session(Id)
);
GO

CREATE INDEX IX_CD_ecom_raw_LoadSessionId
    ON dbo.CD_ecom_raw(LoadSessionId);
GO

/*==============================================================*/
/* 2. Основная таблица                                          */
/*==============================================================*/
CREATE TABLE dbo.CD_ecom
(
    Id                          BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    LoadSessionId               BIGINT                NOT NULL,

    name                        NVARCHAR(255)         NULL,
    [year]                      INT                   NULL,
    season                      INT                   NULL,
    [day]                       INT                   NULL,
    [data]                      DATE                  NULL,
    salesChannelBpo             NVARCHAR(255)         NULL,
    storeRus                    NVARCHAR(255)         NULL,
    mfpDivision                 NVARCHAR(255)         NULL,
    mfpDepartment               NVARCHAR(255)         NULL,
    mfpSubDepartment            NVARCHAR(255)         NULL,
    skuBrandType                NVARCHAR(255)         NULL,
    skuTm                       NVARCHAR(255)         NULL,
    mfpNode                     NVARCHAR(255)         NULL,
    section                     NVARCHAR(255)         NULL,
    merchandiseSubGroup         NVARCHAR(255)         NULL,
    campaignSalesType           NVARCHAR(255)         NULL,
    skuStyleColor               BIGINT                NULL,
    skuPhase                    NVARCHAR(255)         NULL,
    orderPcs                    DECIMAL(18,2)         NULL,
    orderRub                    DECIMAL(18,2)         NULL,
    foundPcs                    DECIMAL(18,2)         NULL,
    foundRub                    DECIMAL(18,2)         NULL,
    salesPcs                    DECIMAL(18,2)         NULL,
    salesRub                    DECIMAL(18,2)         NULL,
    revenue                     DECIMAL(18,2)         NULL,
    gp                          DECIMAL(18,2)         NULL,
    cogs                        DECIMAL(18,2)         NULL,
    salesDiscount               DECIMAL(18,2)         NULL,
    planRub                     BIGINT                NULL,
    stockStoresPcs              BIGINT                NULL,
    stockStoresDdp              BIGINT                NULL,
    cdDrivers                   NVARCHAR(255)         NULL,
    skuSupplierModel            NVARCHAR(255)         NULL,
    skuComposition              NVARCHAR(255)         NULL,
    skuColorRussian             NVARCHAR(255)         NULL,
    skuName                     NVARCHAR(255)         NULL,
    skuCommentBuyer             NVARCHAR(255)         NULL,
    skuCollection               NVARCHAR(255)         NULL,

    CreatedAt                   DATETIME2             NOT NULL CONSTRAINT DF_CD_ecom_CreatedAt DEFAULT SYSUTCDATETIME(),

    CONSTRAINT FK_CD_ecom_LoadSession
        FOREIGN KEY (LoadSessionId) REFERENCES dbo.DWH_Excel_Load_Session(Id)
);
GO

/*==============================================================*/
/* 3. Типизированная staging-таблица                             */
/*==============================================================*/
CREATE TABLE dbo.CD_ecom_stage
(
    LoadSessionId               BIGINT                NOT NULL,
    ExcelRowNum                 BIGINT                NULL,

    name                        NVARCHAR(255)         NULL,
    [year]                      INT                   NULL,
    season                      INT                   NULL,
    [day]                       INT                   NULL,
    [data]                      DATE                  NULL,
    salesChannelBpo             NVARCHAR(255)         NULL,
    storeRus                    NVARCHAR(255)         NULL,
    mfpDivision                 NVARCHAR(255)         NULL,
    mfpDepartment               NVARCHAR(255)         NULL,
    mfpSubDepartment            NVARCHAR(255)         NULL,
    skuBrandType                NVARCHAR(255)         NULL,
    skuTm                       NVARCHAR(255)         NULL,
    mfpNode                     NVARCHAR(255)         NULL,
    section                     NVARCHAR(255)         NULL,
    merchandiseSubGroup         NVARCHAR(255)         NULL,
    campaignSalesType           NVARCHAR(255)         NULL,
    skuStyleColor               BIGINT                NULL,
    skuPhase                    NVARCHAR(255)         NULL,
    orderPcs                    DECIMAL(18,2)         NULL,
    orderRub                    DECIMAL(18,2)         NULL,
    foundPcs                    DECIMAL(18,2)         NULL,
    foundRub                    DECIMAL(18,2)         NULL,
    salesPcs                    DECIMAL(18,2)         NULL,
    salesRub                    DECIMAL(18,2)         NULL,
    revenue                     DECIMAL(18,2)         NULL,
    gp                          DECIMAL(18,2)         NULL,
    cogs                        DECIMAL(18,2)         NULL,
    salesDiscount               DECIMAL(18,2)         NULL,
    planRub                     BIGINT                NULL,
    stockStoresPcs              BIGINT                NULL,
    stockStoresDdp              BIGINT                NULL,
    cdDrivers                   NVARCHAR(255)         NULL,
    skuSupplierModel            NVARCHAR(255)         NULL,
    skuComposition              NVARCHAR(255)         NULL,
    skuColorRussian             NVARCHAR(255)         NULL,
    skuName                     NVARCHAR(255)         NULL,
    skuCommentBuyer             NVARCHAR(255)         NULL,
    skuCollection               NVARCHAR(255)         NULL,

    CONSTRAINT FK_CD_ecom_stage_LoadSession
        FOREIGN KEY (LoadSessionId) REFERENCES dbo.DWH_Excel_Load_Session(Id)
);
GO

CREATE INDEX IX_CD_ecom_LoadSessionId
    ON dbo.CD_ecom(LoadSessionId);
GO

CREATE INDEX IX_CD_ecom_data
    ON dbo.CD_ecom([data]);
GO

CREATE INDEX IX_CD_ecom_storeRus
    ON dbo.CD_ecom(storeRus);
GO

CREATE INDEX IX_CD_ecom_skuTm
    ON dbo.CD_ecom(skuTm);
GO

CREATE INDEX IX_CD_ecom_section
    ON dbo.CD_ecom(section);
GO

CREATE INDEX IX_CD_ecom_stage_LoadSessionId
    ON dbo.CD_ecom_stage(LoadSessionId);
GO
