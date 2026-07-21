USE ReplenishmentDWH;
GO

/* Make dbo.CD_ecom match the common DWH target table contract.
   Java processing always writes LoadSessionId; existing NULL rows must be
   resolved explicitly before this migration is applied. */

IF EXISTS (
    SELECT 1
    FROM dbo.CD_ecom
    WHERE LoadSessionId IS NULL
)
BEGIN
    THROW 51000, 'Cannot alter dbo.CD_ecom.LoadSessionId to NOT NULL because existing NULL rows were found.', 1;
END;
GO

IF EXISTS (
    SELECT 1
    FROM sys.columns c
    WHERE c.object_id = OBJECT_ID(N'dbo.CD_ecom')
      AND c.name = N'LoadSessionId'
      AND c.is_nullable = 1
)
BEGIN
    ALTER TABLE dbo.CD_ecom
        ALTER COLUMN LoadSessionId BIGINT NOT NULL;
END;
GO
