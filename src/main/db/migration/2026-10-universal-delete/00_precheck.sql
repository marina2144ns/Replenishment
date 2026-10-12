SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

IF OBJECT_ID(N'dbo.DWH_Excel_Load_Session', N'U') IS NULL
    THROW 51000, 'dbo.DWH_Excel_Load_Session does not exist', 1;
GO

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'LoadTypeCode') IS NULL
   OR COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'OperationType') IS NULL
   OR COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'Status') IS NULL
   OR COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeletedRows') IS NULL
    THROW 51001, 'dbo.DWH_Excel_Load_Session deletion contract is incomplete', 1;
GO
