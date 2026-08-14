
CREATE TABLE dbo.DWH_Excel_Load_Session
(
    Id              BIGINT IDENTITY(1,1) NOT NULL
        CONSTRAINT PK_DWH_Excel_Load_Session PRIMARY KEY,

    LoadTypeCode    NVARCHAR(50) NOT NULL,
    ServiceName     NVARCHAR(200) NOT NULL,

    FileName        NVARCHAR(500) NULL,
    FilePath        NVARCHAR(1000) NULL,

    OperationType   NVARCHAR(30) NOT NULL
        CONSTRAINT DF_DWH_Excel_Load_Session_OperationType DEFAULT ('LOAD'),
    OperationMode   NVARCHAR(30) NULL,
    DeleteYear      INT NULL,
    DeleteWeek      INT NULL,
    DeleteMonth     INT NULL,
    DeleteYearText  NVARCHAR(50) NULL,
    DeleteMonthText NVARCHAR(50) NULL,
    SourceLoadSessionId BIGINT NULL,
    DeleteCriterion NVARCHAR(50) NULL,
    DeleteParameter1Name NVARCHAR(50) NULL,
    DeleteParameter1Value NVARCHAR(1000) NULL,
    DeleteParameter2Name NVARCHAR(50) NULL,
    DeleteParameter2Value NVARCHAR(1000) NULL,
    DeletedRows     BIGINT NULL,

    Status          NVARCHAR(30) NOT NULL
        CONSTRAINT DF_DWH_Excel_Load_Session_Status DEFAULT ('RUNNING'),

    StartedAt       DATETIME2(0) NOT NULL
        CONSTRAINT DF_DWH_Excel_Load_Session_StartedAt DEFAULT (SYSDATETIME()),

    FinishedAt      DATETIME2(0) NULL,

    Message         NVARCHAR(2000) NULL
);
GO



CREATE TABLE dbo.DWH_Excel_Load_Error
(
    Id              BIGINT IDENTITY(1,1) NOT NULL
        CONSTRAINT PK_DWH_Excel_Load_Error PRIMARY KEY,

    LoadSessionId   BIGINT NOT NULL,

    LoadTypeCode    NVARCHAR(50) NOT NULL,

    ErrorLayer      NVARCHAR(20) NOT NULL,     -- JAVA / SQL

    ExcelRowNum     BIGINT NULL,              -- строка в Excel
    RawId           BIGINT NULL,             -- строка raw таблицы

    FieldName       NVARCHAR(255) NULL,

    ErrorCode       NVARCHAR(100) NULL,

    ErrorReason     NVARCHAR(500) NULL,

    ErrorMessage    NVARCHAR(4000) NOT NULL,

    CreatedAt       DATETIME2(0) NOT NULL
        CONSTRAINT DF_DWH_Excel_Load_Error_CreatedAt DEFAULT (SYSDATETIME()),

    CONSTRAINT FK_DWH_Excel_Load_Error_LoadSession
        FOREIGN KEY (LoadSessionId)
            REFERENCES dbo.DWH_Excel_Load_Session(Id)
);
GO

CREATE INDEX IX_DWH_Excel_Load_Error_LoadSessionId
    ON dbo.DWH_Excel_Load_Error (LoadSessionId);
GO
