IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'OperationType') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session
        ADD OperationType NVARCHAR(30) NOT NULL
            CONSTRAINT DF_DWH_Excel_Load_Session_OperationType DEFAULT ('LOAD');
END;
GO

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'OperationMode') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session
        ADD OperationMode NVARCHAR(30) NULL;
END;
GO

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteYear') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session
        ADD DeleteYear INT NULL;
END;
GO

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeleteWeek') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session
        ADD DeleteWeek INT NULL;
END;
GO

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'SourceLoadSessionId') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session
        ADD SourceLoadSessionId BIGINT NULL;
END;
GO

IF COL_LENGTH(N'dbo.DWH_Excel_Load_Session', N'DeletedRows') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session
        ADD DeletedRows BIGINT NULL;
END;
GO
