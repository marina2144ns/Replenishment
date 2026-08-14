IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Weekly_data') AND name = N'TotalStockPcs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.Weekly_data SET TotalStockPcs = 0 WHERE TotalStockPcs IS NULL;
    ALTER TABLE dbo.Weekly_data ALTER COLUMN TotalStockPcs DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Weekly_data') AND name = N'TotalStockDdp' AND is_nullable = 1)
BEGIN
    UPDATE dbo.Weekly_data SET TotalStockDdp = 0 WHERE TotalStockDdp IS NULL;
    ALTER TABLE dbo.Weekly_data ALTER COLUMN TotalStockDdp DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Weekly_data') AND name = N'SalesPcs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.Weekly_data SET SalesPcs = 0 WHERE SalesPcs IS NULL;
    ALTER TABLE dbo.Weekly_data ALTER COLUMN SalesPcs DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Weekly_data') AND name = N'SalesRub' AND is_nullable = 1)
BEGIN
    UPDATE dbo.Weekly_data SET SalesRub = 0 WHERE SalesRub IS NULL;
    ALTER TABLE dbo.Weekly_data ALTER COLUMN SalesRub DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Weekly_data') AND name = N'Revenue' AND is_nullable = 1)
BEGIN
    UPDATE dbo.Weekly_data SET Revenue = 0 WHERE Revenue IS NULL;
    ALTER TABLE dbo.Weekly_data ALTER COLUMN Revenue DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Weekly_data') AND name = N'Gp' AND is_nullable = 1)
BEGIN
    UPDATE dbo.Weekly_data SET Gp = 0 WHERE Gp IS NULL;
    ALTER TABLE dbo.Weekly_data ALTER COLUMN Gp DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Weekly_data') AND name = N'DiscountTotalRub' AND is_nullable = 1)
BEGIN
    UPDATE dbo.Weekly_data SET DiscountTotalRub = 0 WHERE DiscountTotalRub IS NULL;
    ALTER TABLE dbo.Weekly_data ALTER COLUMN DiscountTotalRub DECIMAL(18,2) NOT NULL;
END;
GO

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Weekly_data_stage') AND name = N'TotalStockPcs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.Weekly_data_stage SET TotalStockPcs = 0 WHERE TotalStockPcs IS NULL;
    ALTER TABLE dbo.Weekly_data_stage ALTER COLUMN TotalStockPcs DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Weekly_data_stage') AND name = N'TotalStockDdp' AND is_nullable = 1)
BEGIN
    UPDATE dbo.Weekly_data_stage SET TotalStockDdp = 0 WHERE TotalStockDdp IS NULL;
    ALTER TABLE dbo.Weekly_data_stage ALTER COLUMN TotalStockDdp DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Weekly_data_stage') AND name = N'SalesPcs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.Weekly_data_stage SET SalesPcs = 0 WHERE SalesPcs IS NULL;
    ALTER TABLE dbo.Weekly_data_stage ALTER COLUMN SalesPcs DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Weekly_data_stage') AND name = N'SalesRub' AND is_nullable = 1)
BEGIN
    UPDATE dbo.Weekly_data_stage SET SalesRub = 0 WHERE SalesRub IS NULL;
    ALTER TABLE dbo.Weekly_data_stage ALTER COLUMN SalesRub DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Weekly_data_stage') AND name = N'Revenue' AND is_nullable = 1)
BEGIN
    UPDATE dbo.Weekly_data_stage SET Revenue = 0 WHERE Revenue IS NULL;
    ALTER TABLE dbo.Weekly_data_stage ALTER COLUMN Revenue DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Weekly_data_stage') AND name = N'Gp' AND is_nullable = 1)
BEGIN
    UPDATE dbo.Weekly_data_stage SET Gp = 0 WHERE Gp IS NULL;
    ALTER TABLE dbo.Weekly_data_stage ALTER COLUMN Gp DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Weekly_data_stage') AND name = N'DiscountTotalRub' AND is_nullable = 1)
BEGIN
    UPDATE dbo.Weekly_data_stage SET DiscountTotalRub = 0 WHERE DiscountTotalRub IS NULL;
    ALTER TABLE dbo.Weekly_data_stage ALTER COLUMN DiscountTotalRub DECIMAL(18,2) NOT NULL;
END;
GO
