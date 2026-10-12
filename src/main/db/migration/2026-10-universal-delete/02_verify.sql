SET NOCOUNT ON;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.DWH_Excel_Load_Session')
      AND name = N'DeleteCriteria'
      AND TYPE_NAME(user_type_id) = N'nvarchar'
      AND max_length = -1
      AND is_nullable = 1
)
    THROW 51010, 'DeleteCriteria must be NVARCHAR(MAX) NULL', 1;
GO
