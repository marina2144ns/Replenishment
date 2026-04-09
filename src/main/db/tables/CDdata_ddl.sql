CREATE TABLE dbo.CD_data (
                             nazvanie                NVARCHAR(255)   NULL,
                             god                     INT             NULL,
                             sezon                   INT             NULL,
                             den                     INT             NULL,
                             data                    DATETIME2       NULL,

                             sales_channel           NVARCHAR(255)   NULL,
                             store_rus               NVARCHAR(255)   NULL,
                             mfp_division            NVARCHAR(255)   NULL,
                             mfp_department          NVARCHAR(255)   NULL,
                             mfp_sub_department      NVARCHAR(255)   NULL,
                             sku_brand_type          NVARCHAR(255)   NULL,
                             sku_tm                  NVARCHAR(255)   NULL,
                             mfp_node                NVARCHAR(255)   NULL,
                             section                 NVARCHAR(255)   NULL,
                             merchandise_sub_group   NVARCHAR(255)   NULL,
                             campaign_sales          NVARCHAR(255)   NULL,

                             sku_style_color         INT             NULL,
                             sku_phase               NVARCHAR(255)   NULL,

                             stock_start_pcs         DECIMAL(18,2)   NULL,
                             stock_start_dd          DECIMAL(18,2)   NULL,

                             sales_pcs               DECIMAL(18,2)   NULL,
                             sales_rub               DECIMAL(18,2)   NULL,
                             revenue                 DECIMAL(18,2)   NULL,
                             gp                      DECIMAL(18,2)   NULL,
                             cogs                    DECIMAL(18,2)   NULL,

                             sales_frp_price         DECIMAL(18,2)   NULL,
                             sales_discount          DECIMAL(18,2)   NULL,

                             stock_stores_pcs        DECIMAL(18,2)   NULL,
                             stock_stores_dd         DECIMAL(18,2)   NULL,

                             plan_rub                INT             NULL,

                             draiverbl_cd            NVARCHAR(255)   NULL,
                             sku_color_rus           NVARCHAR(255)   NULL,
                             sku_composition         NVARCHAR(255)   NULL,
                             sku_supplier            NVARCHAR(255)   NULL,
                             sku_name                NVARCHAR(255)   NULL,
                             sku_collection          NVARCHAR(255)   NULL,
                             sku_comment             NVARCHAR(255)   NULL
);