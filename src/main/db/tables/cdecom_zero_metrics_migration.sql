IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'orderPcs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom SET orderPcs = 0 WHERE orderPcs IS NULL;
    ALTER TABLE dbo.CD_ecom ALTER COLUMN orderPcs DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'orderRub' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom SET orderRub = 0 WHERE orderRub IS NULL;
    ALTER TABLE dbo.CD_ecom ALTER COLUMN orderRub DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'foundPcs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom SET foundPcs = 0 WHERE foundPcs IS NULL;
    ALTER TABLE dbo.CD_ecom ALTER COLUMN foundPcs DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'foundRub' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom SET foundRub = 0 WHERE foundRub IS NULL;
    ALTER TABLE dbo.CD_ecom ALTER COLUMN foundRub DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'salesPcs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom SET salesPcs = 0 WHERE salesPcs IS NULL;
    ALTER TABLE dbo.CD_ecom ALTER COLUMN salesPcs DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'salesRub' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom SET salesRub = 0 WHERE salesRub IS NULL;
    ALTER TABLE dbo.CD_ecom ALTER COLUMN salesRub DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'revenue' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom SET revenue = 0 WHERE revenue IS NULL;
    ALTER TABLE dbo.CD_ecom ALTER COLUMN revenue DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'gp' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom SET gp = 0 WHERE gp IS NULL;
    ALTER TABLE dbo.CD_ecom ALTER COLUMN gp DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'cogs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom SET cogs = 0 WHERE cogs IS NULL;
    ALTER TABLE dbo.CD_ecom ALTER COLUMN cogs DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'salesDiscount' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom SET salesDiscount = 0 WHERE salesDiscount IS NULL;
    ALTER TABLE dbo.CD_ecom ALTER COLUMN salesDiscount DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'planRub' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom SET planRub = 0 WHERE planRub IS NULL;
    ALTER TABLE dbo.CD_ecom ALTER COLUMN planRub BIGINT NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'stockStoresPcs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom SET stockStoresPcs = 0 WHERE stockStoresPcs IS NULL;
    ALTER TABLE dbo.CD_ecom ALTER COLUMN stockStoresPcs BIGINT NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom') AND name = N'stockStoresDdp' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom SET stockStoresDdp = 0 WHERE stockStoresDdp IS NULL;
    ALTER TABLE dbo.CD_ecom ALTER COLUMN stockStoresDdp BIGINT NOT NULL;
END;
GO

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'orderPcs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom_stage SET orderPcs = 0 WHERE orderPcs IS NULL;
    ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN orderPcs DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'orderRub' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom_stage SET orderRub = 0 WHERE orderRub IS NULL;
    ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN orderRub DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'foundPcs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom_stage SET foundPcs = 0 WHERE foundPcs IS NULL;
    ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN foundPcs DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'foundRub' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom_stage SET foundRub = 0 WHERE foundRub IS NULL;
    ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN foundRub DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'salesPcs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom_stage SET salesPcs = 0 WHERE salesPcs IS NULL;
    ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN salesPcs DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'salesRub' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom_stage SET salesRub = 0 WHERE salesRub IS NULL;
    ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN salesRub DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'revenue' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom_stage SET revenue = 0 WHERE revenue IS NULL;
    ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN revenue DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'gp' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom_stage SET gp = 0 WHERE gp IS NULL;
    ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN gp DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'cogs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom_stage SET cogs = 0 WHERE cogs IS NULL;
    ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN cogs DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'salesDiscount' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom_stage SET salesDiscount = 0 WHERE salesDiscount IS NULL;
    ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN salesDiscount DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'planRub' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom_stage SET planRub = 0 WHERE planRub IS NULL;
    ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN planRub BIGINT NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'stockStoresPcs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom_stage SET stockStoresPcs = 0 WHERE stockStoresPcs IS NULL;
    ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN stockStoresPcs BIGINT NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_ecom_stage') AND name = N'stockStoresDdp' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_ecom_stage SET stockStoresDdp = 0 WHERE stockStoresDdp IS NULL;
    ALTER TABLE dbo.CD_ecom_stage ALTER COLUMN stockStoresDdp BIGINT NOT NULL;
END;
GO
