CREATE TABLE dbo.StoreTurnover (
    Id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    -- Transitional coexistence contract: legacy v1 may still insert nullable business values.
    -- Successful v2 rows remain non-null through Java validation and strict STAGE schema.
    sku NVARCHAR(255) NULL,
    period DATE NULL,
    storeRus NVARCHAR(255) NULL,
    remainingSum INT NULL,
    remainingDays INT NULL,
    salesQuantity INT NULL,
    sales INT NULL,
    asp INT NULL,
    revenue INT NULL,
    gp INT NULL,
    discountTotal INT NULL,
    LoadSessionId BIGINT NULL,
    RawRowId BIGINT NULL,
    LoadDateTime DATETIME2(0) NOT NULL
        CONSTRAINT DF_StoreTurnover_LoadDateTime DEFAULT SYSDATETIME(),
    CONSTRAINT FK_StoreTurnover_LoadSession
        FOREIGN KEY (LoadSessionId) REFERENCES dbo.DWH_Excel_Load_Session(Id)
);
GO

CREATE TABLE dbo.StoreTurnover_raw (
    Id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    LoadSessionId BIGINT NOT NULL,
    ExcelRowNum BIGINT NULL,
    sku NVARCHAR(4000) NULL,
    period NVARCHAR(4000) NULL,
    storeRus NVARCHAR(4000) NULL,
    remainingSum NVARCHAR(4000) NULL,
    remainingDays NVARCHAR(4000) NULL,
    salesQuantity NVARCHAR(4000) NULL,
    sales NVARCHAR(4000) NULL,
    asp NVARCHAR(4000) NULL,
    revenue NVARCHAR(4000) NULL,
    gp NVARCHAR(4000) NULL,
    discountTotal NVARCHAR(4000) NULL,
    CreatedAt DATETIME2(0) NOT NULL
        CONSTRAINT DF_StoreTurnover_raw_CreatedAt DEFAULT SYSDATETIME(),
    CONSTRAINT FK_StoreTurnover_raw_LoadSession
        FOREIGN KEY (LoadSessionId) REFERENCES dbo.DWH_Excel_Load_Session(Id)
);
GO

CREATE TABLE dbo.StoreTurnover_stage (
    LoadSessionId BIGINT NOT NULL,
    ExcelRowNum BIGINT NULL,
    sku NVARCHAR(255) NOT NULL,
    period DATE NOT NULL,
    storeRus NVARCHAR(255) NOT NULL,
    remainingSum INT NOT NULL,
    remainingDays INT NOT NULL,
    salesQuantity INT NOT NULL,
    sales INT NOT NULL,
    asp INT NOT NULL,
    revenue INT NOT NULL,
    gp INT NOT NULL,
    discountTotal INT NOT NULL,
    RawRowId BIGINT NOT NULL,
    CONSTRAINT FK_StoreTurnover_stage_LoadSession
        FOREIGN KEY (LoadSessionId) REFERENCES dbo.DWH_Excel_Load_Session(Id),
    CONSTRAINT FK_StoreTurnover_stage_RawRow
        FOREIGN KEY (RawRowId) REFERENCES dbo.StoreTurnover_raw(Id)
);
GO

CREATE INDEX IX_StoreTurnover_LoadSessionId ON dbo.StoreTurnover(LoadSessionId);
GO
CREATE INDEX IX_StoreTurnover_raw_LoadSessionId_Id ON dbo.StoreTurnover_raw(LoadSessionId, Id);
GO
CREATE INDEX IX_StoreTurnover_stage_LoadSessionId ON dbo.StoreTurnover_stage(LoadSessionId);
GO
