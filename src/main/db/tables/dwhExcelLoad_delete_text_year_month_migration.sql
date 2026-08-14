IF COL_LENGTH('dbo.DWH_Excel_Load_Session', 'DeleteYearText') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session
        ADD DeleteYearText NVARCHAR(50) NULL;
END;
GO

IF COL_LENGTH('dbo.DWH_Excel_Load_Session', 'DeleteMonthText') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session
        ADD DeleteMonthText NVARCHAR(50) NULL;
END;
GO
