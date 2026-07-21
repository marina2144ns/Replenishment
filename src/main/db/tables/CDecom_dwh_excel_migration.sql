USE ReplenishmentDWH;
GO

/* Safe migration for CDEcom to the common DWH Excel load framework. */

IF COL_LENGTH('dbo.CD_ecom_raw', 'ExcelRowNum') IS NULL
BEGIN
    ALTER TABLE dbo.CD_ecom_raw
        ADD ExcelRowNum BIGINT NULL;
END;
GO

ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN name NVARCHAR(4000) NULL;
ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN salesChannelBpo NVARCHAR(4000) NULL;
ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN storeRus NVARCHAR(4000) NULL;
ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN mfpDivision NVARCHAR(4000) NULL;
ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN mfpDepartment NVARCHAR(4000) NULL;
ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN mfpSubDepartment NVARCHAR(4000) NULL;
ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN skuBrandType NVARCHAR(4000) NULL;
ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN skuTm NVARCHAR(4000) NULL;
ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN mfpNode NVARCHAR(4000) NULL;
ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN section NVARCHAR(4000) NULL;
ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN merchandiseSubGroup NVARCHAR(4000) NULL;
ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN campaignSalesType NVARCHAR(4000) NULL;
ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN skuPhase NVARCHAR(4000) NULL;
ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN cdDrivers NVARCHAR(4000) NULL;
ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN skuSupplierModel NVARCHAR(4000) NULL;
ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN skuComposition NVARCHAR(4000) NULL;
ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN skuColorRussian NVARCHAR(4000) NULL;
ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN skuName NVARCHAR(4000) NULL;
ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN skuCommentBuyer NVARCHAR(4000) NULL;
ALTER TABLE dbo.CD_ecom_raw ALTER COLUMN skuCollection NVARCHAR(4000) NULL;
GO

IF EXISTS (
        SELECT 1
        FROM sys.foreign_keys
        WHERE name = N'FK_CD_ecom_raw_LoadSession'
          AND parent_object_id = OBJECT_ID(N'dbo.CD_ecom_raw')
    )
BEGIN
    ALTER TABLE dbo.CD_ecom_raw
        DROP CONSTRAINT FK_CD_ecom_raw_LoadSession;
END;
GO

ALTER TABLE dbo.CD_ecom_raw
    ADD CONSTRAINT FK_CD_ecom_raw_LoadSession
        FOREIGN KEY (LoadSessionId) REFERENCES dbo.DWH_Excel_Load_Session(Id);
GO

IF EXISTS (
        SELECT 1
        FROM sys.foreign_keys
        WHERE name = N'FK_CD_ecom_LoadSession'
          AND parent_object_id = OBJECT_ID(N'dbo.CD_ecom')
    )
BEGIN
    ALTER TABLE dbo.CD_ecom
        DROP CONSTRAINT FK_CD_ecom_LoadSession;
END;
GO

ALTER TABLE dbo.CD_ecom
    ADD CONSTRAINT FK_CD_ecom_LoadSession
        FOREIGN KEY (LoadSessionId) REFERENCES dbo.DWH_Excel_Load_Session(Id);
GO
