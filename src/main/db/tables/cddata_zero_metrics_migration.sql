IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'stock_start_pcs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data SET stock_start_pcs = 0 WHERE stock_start_pcs IS NULL;
    ALTER TABLE dbo.CD_data ALTER COLUMN stock_start_pcs DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'stock_start_dd' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data SET stock_start_dd = 0 WHERE stock_start_dd IS NULL;
    ALTER TABLE dbo.CD_data ALTER COLUMN stock_start_dd DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'sales_pcs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data SET sales_pcs = 0 WHERE sales_pcs IS NULL;
    ALTER TABLE dbo.CD_data ALTER COLUMN sales_pcs DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'sales_rub' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data SET sales_rub = 0 WHERE sales_rub IS NULL;
    ALTER TABLE dbo.CD_data ALTER COLUMN sales_rub DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'revenue' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data SET revenue = 0 WHERE revenue IS NULL;
    ALTER TABLE dbo.CD_data ALTER COLUMN revenue DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'gp' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data SET gp = 0 WHERE gp IS NULL;
    ALTER TABLE dbo.CD_data ALTER COLUMN gp DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'cogs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data SET cogs = 0 WHERE cogs IS NULL;
    ALTER TABLE dbo.CD_data ALTER COLUMN cogs DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'sales_frp_price' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data SET sales_frp_price = 0 WHERE sales_frp_price IS NULL;
    ALTER TABLE dbo.CD_data ALTER COLUMN sales_frp_price DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'sales_discount' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data SET sales_discount = 0 WHERE sales_discount IS NULL;
    ALTER TABLE dbo.CD_data ALTER COLUMN sales_discount DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'stock_stores_pcs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data SET stock_stores_pcs = 0 WHERE stock_stores_pcs IS NULL;
    ALTER TABLE dbo.CD_data ALTER COLUMN stock_stores_pcs DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'stock_stores_dd' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data SET stock_stores_dd = 0 WHERE stock_stores_dd IS NULL;
    ALTER TABLE dbo.CD_data ALTER COLUMN stock_stores_dd DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data') AND name = N'plan_rub' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data SET plan_rub = 0 WHERE plan_rub IS NULL;
    ALTER TABLE dbo.CD_data ALTER COLUMN plan_rub INT NOT NULL;
END;
GO

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data_stage') AND name = N'stock_start_pcs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data_stage SET stock_start_pcs = 0 WHERE stock_start_pcs IS NULL;
    ALTER TABLE dbo.CD_data_stage ALTER COLUMN stock_start_pcs DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data_stage') AND name = N'stock_start_dd' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data_stage SET stock_start_dd = 0 WHERE stock_start_dd IS NULL;
    ALTER TABLE dbo.CD_data_stage ALTER COLUMN stock_start_dd DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data_stage') AND name = N'sales_pcs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data_stage SET sales_pcs = 0 WHERE sales_pcs IS NULL;
    ALTER TABLE dbo.CD_data_stage ALTER COLUMN sales_pcs DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data_stage') AND name = N'sales_rub' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data_stage SET sales_rub = 0 WHERE sales_rub IS NULL;
    ALTER TABLE dbo.CD_data_stage ALTER COLUMN sales_rub DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data_stage') AND name = N'revenue' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data_stage SET revenue = 0 WHERE revenue IS NULL;
    ALTER TABLE dbo.CD_data_stage ALTER COLUMN revenue DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data_stage') AND name = N'gp' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data_stage SET gp = 0 WHERE gp IS NULL;
    ALTER TABLE dbo.CD_data_stage ALTER COLUMN gp DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data_stage') AND name = N'cogs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data_stage SET cogs = 0 WHERE cogs IS NULL;
    ALTER TABLE dbo.CD_data_stage ALTER COLUMN cogs DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data_stage') AND name = N'sales_frp_price' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data_stage SET sales_frp_price = 0 WHERE sales_frp_price IS NULL;
    ALTER TABLE dbo.CD_data_stage ALTER COLUMN sales_frp_price DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data_stage') AND name = N'sales_discount' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data_stage SET sales_discount = 0 WHERE sales_discount IS NULL;
    ALTER TABLE dbo.CD_data_stage ALTER COLUMN sales_discount DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data_stage') AND name = N'stock_stores_pcs' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data_stage SET stock_stores_pcs = 0 WHERE stock_stores_pcs IS NULL;
    ALTER TABLE dbo.CD_data_stage ALTER COLUMN stock_stores_pcs DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data_stage') AND name = N'stock_stores_dd' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data_stage SET stock_stores_dd = 0 WHERE stock_stores_dd IS NULL;
    ALTER TABLE dbo.CD_data_stage ALTER COLUMN stock_stores_dd DECIMAL(18,2) NOT NULL;
END;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CD_data_stage') AND name = N'plan_rub' AND is_nullable = 1)
BEGIN
    UPDATE dbo.CD_data_stage SET plan_rub = 0 WHERE plan_rub IS NULL;
    ALTER TABLE dbo.CD_data_stage ALTER COLUMN plan_rub INT NOT NULL;
END;
GO
