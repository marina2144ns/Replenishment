USE ReplenishmentDWH;
GO

/* Safe migration for CDEcom to the common DWH Excel load framework.
   Legacy dbo.CD_ecom_load_session and dbo.CD_ecom_load_error are kept
   during production verification and can be removed in a later cleanup. */

IF COL_LENGTH('dbo.CD_ecom_raw', 'ExcelRowNum') IS NULL
BEGIN
    ALTER TABLE dbo.CD_ecom_raw
        ADD ExcelRowNum BIGINT NULL;
END;
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
