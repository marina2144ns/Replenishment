IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.SalesByChannel') AND name = N'salesQuantity' AND is_nullable = 1)
BEGIN
    UPDATE dbo.SalesByChannel SET salesQuantity = 0 WHERE salesQuantity IS NULL;
    ALTER TABLE dbo.SalesByChannel ALTER COLUMN salesQuantity INT NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.SalesByChannel') AND name = N'salesCurr' AND is_nullable = 1)
BEGIN
    UPDATE dbo.SalesByChannel SET salesCurr = 0 WHERE salesCurr IS NULL;
    ALTER TABLE dbo.SalesByChannel ALTER COLUMN salesCurr DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.SalesByChannel') AND name = N'gm' AND is_nullable = 1)
BEGIN
    UPDATE dbo.SalesByChannel SET gm = 0 WHERE gm IS NULL;
    ALTER TABLE dbo.SalesByChannel ALTER COLUMN gm DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.SalesByChannel') AND name = N'discountTtl' AND is_nullable = 1)
BEGIN
    UPDATE dbo.SalesByChannel SET discountTtl = 0 WHERE discountTtl IS NULL;
    ALTER TABLE dbo.SalesByChannel ALTER COLUMN discountTtl DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.SalesByChannel') AND name = N'turnoverCurr' AND is_nullable = 1)
BEGIN
    UPDATE dbo.SalesByChannel SET turnoverCurr = 0 WHERE turnoverCurr IS NULL;
    ALTER TABLE dbo.SalesByChannel ALTER COLUMN turnoverCurr DECIMAL(18,2) NOT NULL;
END;
GO

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.SalesByChannel_stage') AND name = N'salesQuantity' AND is_nullable = 1)
BEGIN
    UPDATE dbo.SalesByChannel_stage SET salesQuantity = 0 WHERE salesQuantity IS NULL;
    ALTER TABLE dbo.SalesByChannel_stage ALTER COLUMN salesQuantity INT NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.SalesByChannel_stage') AND name = N'salesCurr' AND is_nullable = 1)
BEGIN
    UPDATE dbo.SalesByChannel_stage SET salesCurr = 0 WHERE salesCurr IS NULL;
    ALTER TABLE dbo.SalesByChannel_stage ALTER COLUMN salesCurr DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.SalesByChannel_stage') AND name = N'gm' AND is_nullable = 1)
BEGIN
    UPDATE dbo.SalesByChannel_stage SET gm = 0 WHERE gm IS NULL;
    ALTER TABLE dbo.SalesByChannel_stage ALTER COLUMN gm DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.SalesByChannel_stage') AND name = N'discountTtl' AND is_nullable = 1)
BEGIN
    UPDATE dbo.SalesByChannel_stage SET discountTtl = 0 WHERE discountTtl IS NULL;
    ALTER TABLE dbo.SalesByChannel_stage ALTER COLUMN discountTtl DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.SalesByChannel_stage') AND name = N'turnoverCurr' AND is_nullable = 1)
BEGIN
    UPDATE dbo.SalesByChannel_stage SET turnoverCurr = 0 WHERE turnoverCurr IS NULL;
    ALTER TABLE dbo.SalesByChannel_stage ALTER COLUMN turnoverCurr DECIMAL(18,2) NOT NULL;
END;
GO
