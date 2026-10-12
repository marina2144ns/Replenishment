SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteCriteria') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session
        ADD DeleteCriteria NVARCHAR(MAX) NULL;
END;
GO
