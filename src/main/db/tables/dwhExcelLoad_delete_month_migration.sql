IF COL_LENGTH('dbo.DWH_Excel_Load_Session', 'DeleteMonth') IS NULL
BEGIN
    ALTER TABLE dbo.DWH_Excel_Load_Session
        ADD DeleteMonth INT NULL;
END;
GO
