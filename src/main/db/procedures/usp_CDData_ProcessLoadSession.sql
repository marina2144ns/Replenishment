
CREATE PROCEDURE dbo.usp_CDData_ProcessLoadSession
@LoadSessionId BIGINT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE
        @LoadTypeCode NVARCHAR(100) = N'CD_DATA',
        @TotalRows    BIGINT = 0,
        @LoadedRows   BIGINT = 0,
        @ErrorRows    BIGINT = 0,
        @Success      BIT = 0,
        @Message      NVARCHAR(2000);

    BEGIN TRY

        /* 1. Проверка существования общей сессии */
        IF NOT EXISTS
            (
                SELECT 1
                FROM dbo.DWH_Excel_Load_Session
                WHERE Id = @LoadSessionId
            )
            BEGIN
                SET @Message = CONCAT(N'LoadSessionId ', @LoadSessionId, N' not found.');

                SELECT
                    @LoadSessionId AS LoadSessionId,
                    CAST(0 AS BIT) AS Success,
                    CAST(0 AS BIGINT) AS TotalRows,
                    CAST(0 AS BIGINT) AS LoadedRows,
                    CAST(0 AS BIGINT) AS ErrorRows,
                    @Message AS Message;

                RETURN;
            END;

        /* 2. Очистка предыдущего результата обработки по этой сессии */
        DELETE FROM dbo.DWH_Excel_Load_Error
        WHERE LoadSessionId = @LoadSessionId
          AND LoadTypeCode = @LoadTypeCode;

        DELETE FROM dbo.CD_data
        WHERE LoadSessionId = @LoadSessionId;

        /* 3. Подсчёт raw-строк */
        SELECT
                @TotalRows = COUNT_BIG(*)
        FROM dbo.CD_data_raw
        WHERE LoadSessionId = @LoadSessionId;

        /* 4. Валидация */
        ;WITH Src AS
                  (
                      SELECT
                          r.Id,
                          r.LoadSessionId,
                          r.ExcelRowNum,

                          r.nazvanie,
                          r.god,
                          r.sezon,
                          r.den,
                          r.data,
                          r.sales_channel,
                          r.store_rus,
                          r.mfp_division,
                          r.mfp_department,
                          r.mfp_sub_department,
                          r.sku_brand_type,
                          r.sku_tm,
                          r.mfp_node,
                          r.section,
                          r.merchandise_sub_group,
                          r.campaign_sales,
                          r.sku_style_color,
                          r.sku_phase,
                          r.stock_start_pcs,
                          r.stock_start_dd,
                          r.sales_pcs,
                          r.sales_rub,
                          r.revenue,
                          r.gp,
                          r.cogs,
                          r.sales_frp_price,
                          r.sales_discount,
                          r.stock_stores_pcs,
                          r.stock_stores_dd,
                          r.plan_rub,
                          r.draivery_cd,
                          r.sku_color_rus,
                          r.sku_composition,
                          r.sku_supplier,
                          r.sku_name,
                          r.sku_collection,
                          r.sku_comment,

                          nazvanie_clean = NULLIF(LTRIM(RTRIM(r.nazvanie)), N''),
                          sales_channel_clean = NULLIF(LTRIM(RTRIM(r.sales_channel)), N''),
                          store_rus_clean = NULLIF(LTRIM(RTRIM(r.store_rus)), N''),
                          mfp_division_clean = NULLIF(LTRIM(RTRIM(r.mfp_division)), N''),
                          mfp_department_clean = NULLIF(LTRIM(RTRIM(r.mfp_department)), N''),
                          mfp_sub_department_clean = NULLIF(LTRIM(RTRIM(r.mfp_sub_department)), N''),
                          sku_brand_type_clean = NULLIF(LTRIM(RTRIM(r.sku_brand_type)), N''),
                          sku_tm_clean = NULLIF(LTRIM(RTRIM(r.sku_tm)), N''),
                          mfp_node_clean = NULLIF(LTRIM(RTRIM(r.mfp_node)), N''),
                          section_clean = NULLIF(LTRIM(RTRIM(r.section)), N''),
                          merchandise_sub_group_clean = NULLIF(LTRIM(RTRIM(r.merchandise_sub_group)), N''),
                          campaign_sales_clean = NULLIF(LTRIM(RTRIM(r.campaign_sales)), N''),
                          sku_phase_clean = NULLIF(LTRIM(RTRIM(r.sku_phase)), N''),
                          draivery_cd_clean = NULLIF(LTRIM(RTRIM(r.draivery_cd)), N''),
                          sku_color_rus_clean = NULLIF(LTRIM(RTRIM(r.sku_color_rus)), N''),
                          sku_composition_clean = NULLIF(LTRIM(RTRIM(r.sku_composition)), N''),
                          sku_supplier_clean = NULLIF(LTRIM(RTRIM(r.sku_supplier)), N''),
                          sku_name_clean = NULLIF(LTRIM(RTRIM(r.sku_name)), N''),
                          sku_collection_clean = NULLIF(LTRIM(RTRIM(r.sku_collection)), N''),
                          sku_comment_clean = NULLIF(LTRIM(RTRIM(r.sku_comment)), N''),

                          god_clean = NULLIF(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.god)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N''),
                          sezon_clean = NULLIF(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sezon)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N''),
                          den_clean = NULLIF(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.den)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N''),
                          sku_style_color_clean = NULLIF(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sku_style_color)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N''),
                          plan_rub_clean = NULLIF(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.plan_rub)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N''),

                          data_clean = NULLIF(LTRIM(RTRIM(r.data)), N''),

                          stock_start_pcs_clean = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.stock_start_pcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'), N''),
                          stock_start_dd_clean = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.stock_start_dd)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'), N''),
                          sales_pcs_clean = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sales_pcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'), N''),
                          sales_rub_clean = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sales_rub)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'), N''),
                          revenue_clean = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.revenue)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'), N''),
                          gp_clean = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.gp)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'), N''),
                          cogs_clean = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.cogs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'), N''),
                          sales_frp_price_clean = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sales_frp_price)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'), N''),
                          sales_discount_clean = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sales_discount)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'), N''),
                          stock_stores_pcs_clean = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.stock_stores_pcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'), N''),
                          stock_stores_dd_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(
                                                  REPLACE(
                                                          REPLACE(LTRIM(RTRIM(r.stock_stores_dd)), NCHAR(160), N''),
                                                          NCHAR(8239), N''
                                                      ),
                                                  N' ', N''
                                              ),
                                          N',', N'.'
                                      ),
                                  N''
                              )
                                                         FROM dbo.CD_data_raw r
                                                         WHERE r.LoadSessionId = @LoadSessionId
                              ),
                          Typed AS
        (
            SELECT
                s.*,

                god_float = TRY_CONVERT(FLOAT, s.god_clean),
                sezon_float = TRY_CONVERT(FLOAT, s.sezon_clean),
                den_float = TRY_CONVERT(FLOAT, s.den_clean),
                sku_style_color_float = TRY_CONVERT(FLOAT, s.sku_style_color_clean),
                plan_rub_float = TRY_CONVERT(FLOAT, s.plan_rub_clean),

                stock_start_pcs_float = TRY_CONVERT(FLOAT, s.stock_start_pcs_clean),
                stock_start_dd_float = TRY_CONVERT(FLOAT, s.stock_start_dd_clean),
                sales_pcs_float = TRY_CONVERT(FLOAT, s.sales_pcs_clean),
                sales_rub_float = TRY_CONVERT(FLOAT, s.sales_rub_clean),
                revenue_float = TRY_CONVERT(FLOAT, s.revenue_clean),
                gp_float = TRY_CONVERT(FLOAT, s.gp_clean),
                cogs_float = TRY_CONVERT(FLOAT, s.cogs_clean),
                sales_frp_price_float = TRY_CONVERT(FLOAT, s.sales_frp_price_clean),
                sales_discount_float = TRY_CONVERT(FLOAT, s.sales_discount_clean),
                stock_stores_pcs_float = TRY_CONVERT(FLOAT, s.stock_stores_pcs_clean),
                stock_stores_dd_float = TRY_CONVERT(FLOAT, s.stock_stores_dd_clean),

                data_value =
                    COALESCE(
                        TRY_CONVERT(DATE, s.data_clean, 104),
                        TRY_CONVERT(DATE, s.data_clean, 103),
                        TRY_CONVERT(DATE, s.data_clean, 23),
                        TRY_CONVERT(DATE, s.data_clean, 120),
                        TRY_CONVERT(DATE, s.data_clean, 1),
                        CASE
                            WHEN TRY_CONVERT(INT, s.data_clean) IS NOT NULL
                                 AND TRY_CONVERT(INT, s.data_clean) BETWEEN 1 AND 60000
                            THEN DATEADD(DAY, TRY_CONVERT(INT, s.data_clean) - 2, CONVERT(DATE, '19000101', 112))
                            ELSE NULL
                        END
                    )
            FROM Src s
        ),
                              Validation AS
        (
            SELECT
                t.Id,
                t.ExcelRowNum,

                FieldName =
                    CASE
                        WHEN t.god_clean IS NOT NULL
                             AND (
                                    t.god_float IS NULL
                                    OR t.god_float <> FLOOR(t.god_float)
                                    OR ABS(t.god_float) > 2147483647
                                 ) THEN N'god'

                        WHEN t.sezon_clean IS NOT NULL
                             AND (
                                    t.sezon_float IS NULL
                                    OR t.sezon_float <> FLOOR(t.sezon_float)
                                    OR ABS(t.sezon_float) > 2147483647
                                 ) THEN N'sezon'

                        WHEN t.den_clean IS NOT NULL
                             AND (
                                    t.den_float IS NULL
                                    OR t.den_float <> FLOOR(t.den_float)
                                    OR ABS(t.den_float) > 2147483647
                                 ) THEN N'den'

                        WHEN t.data_clean IS NOT NULL
                             AND t.data_value IS NULL THEN N'data'

                        WHEN t.sku_style_color_clean IS NOT NULL
                             AND (
                                    t.sku_style_color_float IS NULL
                                    OR t.sku_style_color_float <> FLOOR(t.sku_style_color_float)
                                    OR ABS(t.sku_style_color_float) > 2147483647
                                 ) THEN N'sku_style_color'

                        WHEN t.plan_rub_clean IS NOT NULL
                             AND (
                                    t.plan_rub_float IS NULL
                                    OR t.plan_rub_float <> FLOOR(t.plan_rub_float)
                                    OR ABS(t.plan_rub_float) > 2147483647
                                 ) THEN N'plan_rub'

                        WHEN t.stock_start_pcs_clean IS NOT NULL
                             AND (t.stock_start_pcs_float IS NULL OR ABS(ROUND(t.stock_start_pcs_float, 2)) > 9999999999999999.99) THEN N'stock_start_pcs'

                        WHEN t.stock_start_dd_clean IS NOT NULL
                             AND (t.stock_start_dd_float IS NULL OR ABS(ROUND(t.stock_start_dd_float, 2)) > 9999999999999999.99) THEN N'stock_start_dd'

                        WHEN t.sales_pcs_clean IS NOT NULL
                             AND (t.sales_pcs_float IS NULL OR ABS(ROUND(t.sales_pcs_float, 2)) > 9999999999999999.99) THEN N'sales_pcs'

                        WHEN t.sales_rub_clean IS NOT NULL
                             AND (t.sales_rub_float IS NULL OR ABS(ROUND(t.sales_rub_float, 2)) > 9999999999999999.99) THEN N'sales_rub'

                        WHEN t.revenue_clean IS NOT NULL
                             AND (t.revenue_float IS NULL OR ABS(ROUND(t.revenue_float, 2)) > 9999999999999999.99) THEN N'revenue'

                        WHEN t.gp_clean IS NOT NULL
                             AND (t.gp_float IS NULL OR ABS(ROUND(t.gp_float, 2)) > 9999999999999999.99) THEN N'gp'

                        WHEN t.cogs_clean IS NOT NULL
                             AND (t.cogs_float IS NULL OR ABS(ROUND(t.cogs_float, 2)) > 9999999999999999.99) THEN N'cogs'

                        WHEN t.sales_frp_price_clean IS NOT NULL
                             AND (t.sales_frp_price_float IS NULL OR ABS(ROUND(t.sales_frp_price_float, 2)) > 9999999999999999.99) THEN N'sales_frp_price'

                        WHEN t.sales_discount_clean IS NOT NULL
                             AND (t.sales_discount_float IS NULL OR ABS(ROUND(t.sales_discount_float, 2)) > 9999999999999999.99) THEN N'sales_discount'

                        WHEN t.stock_stores_pcs_clean IS NOT NULL
                             AND (t.stock_stores_pcs_float IS NULL OR ABS(ROUND(t.stock_stores_pcs_float, 2)) > 9999999999999999.99) THEN N'stock_stores_pcs'

                        WHEN t.stock_stores_dd_clean IS NOT NULL
                             AND (t.stock_stores_dd_float IS NULL OR ABS(ROUND(t.stock_stores_dd_float, 2)) > 9999999999999999.99) THEN N'stock_stores_dd'

                        WHEN t.nazvanie_clean IS NOT NULL AND LEN(t.nazvanie_clean) > 255 THEN N'nazvanie'
                        WHEN t.sales_channel_clean IS NOT NULL AND LEN(t.sales_channel_clean) > 255 THEN N'sales_channel'
                        WHEN t.store_rus_clean IS NOT NULL AND LEN(t.store_rus_clean) > 255 THEN N'store_rus'
                        WHEN t.mfp_division_clean IS NOT NULL AND LEN(t.mfp_division_clean) > 255 THEN N'mfp_division'
                        WHEN t.mfp_department_clean IS NOT NULL AND LEN(t.mfp_department_clean) > 255 THEN N'mfp_department'
                        WHEN t.mfp_sub_department_clean IS NOT NULL AND LEN(t.mfp_sub_department_clean) > 255 THEN N'mfp_sub_department'
                        WHEN t.sku_brand_type_clean IS NOT NULL AND LEN(t.sku_brand_type_clean) > 255 THEN N'sku_brand_type'
                        WHEN t.sku_tm_clean IS NOT NULL AND LEN(t.sku_tm_clean) > 255 THEN N'sku_tm'
                        WHEN t.mfp_node_clean IS NOT NULL AND LEN(t.mfp_node_clean) > 255 THEN N'mfp_node'
                        WHEN t.section_clean IS NOT NULL AND LEN(t.section_clean) > 255 THEN N'section'
                        WHEN t.merchandise_sub_group_clean IS NOT NULL AND LEN(t.merchandise_sub_group_clean) > 255 THEN N'merchandise_sub_group'
                        WHEN t.campaign_sales_clean IS NOT NULL AND LEN(t.campaign_sales_clean) > 255 THEN N'campaign_sales'
                        WHEN t.sku_phase_clean IS NOT NULL AND LEN(t.sku_phase_clean) > 255 THEN N'sku_phase'
                        WHEN t.draivery_cd_clean IS NOT NULL AND LEN(t.draivery_cd_clean) > 255 THEN N'draivery_cd'
                        WHEN t.sku_color_rus_clean IS NOT NULL AND LEN(t.sku_color_rus_clean) > 255 THEN N'sku_color_rus'
                        WHEN t.sku_composition_clean IS NOT NULL AND LEN(t.sku_composition_clean) > 255 THEN N'sku_composition'
                        WHEN t.sku_supplier_clean IS NOT NULL AND LEN(t.sku_supplier_clean) > 255 THEN N'sku_supplier'
                        WHEN t.sku_name_clean IS NOT NULL AND LEN(t.sku_name_clean) > 255 THEN N'sku_name'
                        WHEN t.sku_collection_clean IS NOT NULL AND LEN(t.sku_collection_clean) > 255 THEN N'sku_collection'
                        WHEN t.sku_comment_clean IS NOT NULL AND LEN(t.sku_comment_clean) > 255 THEN N'sku_comment'

                        ELSE NULL
                    END,

                ErrorMessage =
                    CASE
                        WHEN t.god_clean IS NOT NULL
                             AND (
                                    t.god_float IS NULL
                                    OR t.god_float <> FLOOR(t.god_float)
                                    OR ABS(t.god_float) > 2147483647
                                 )
                            THEN CONCAT(N'RawId=', t.Id, N'. Invalid INT value in [god]: [', t.god, N']')

                        WHEN t.sezon_clean IS NOT NULL
                             AND (
                                    t.sezon_float IS NULL
                                    OR t.sezon_float <> FLOOR(t.sezon_float)
                                    OR ABS(t.sezon_float) > 2147483647
                                 )
                            THEN CONCAT(N'RawId=', t.Id, N'. Invalid INT value in [sezon]: [', t.sezon, N']')

                        WHEN t.den_clean IS NOT NULL
                             AND (
                                    t.den_float IS NULL
                                    OR t.den_float <> FLOOR(t.den_float)
                                    OR ABS(t.den_float) > 2147483647
                                 )
                            THEN CONCAT(N'RawId=', t.Id, N'. Invalid INT value in [den]: [', t.den, N']')

                        WHEN t.data_clean IS NOT NULL
                             AND t.data_value IS NULL
                            THEN CONCAT(N'RawId=', t.Id, N'. Invalid DATE value in [data]: [', t.data, N']')

                        WHEN t.sku_style_color_clean IS NOT NULL
                             AND (
                                    t.sku_style_color_float IS NULL
                                    OR t.sku_style_color_float <> FLOOR(t.sku_style_color_float)
                                    OR ABS(t.sku_style_color_float) > 2147483647
                                 )
                            THEN CONCAT(N'RawId=', t.Id, N'. Invalid INT value in [sku_style_color]: [', t.sku_style_color, N']')

                        WHEN t.plan_rub_clean IS NOT NULL
                             AND (
                                    t.plan_rub_float IS NULL
                                    OR t.plan_rub_float <> FLOOR(t.plan_rub_float)
                                    OR ABS(t.plan_rub_float) > 2147483647
                                 )
                            THEN CONCAT(N'RawId=', t.Id, N'. Invalid INT value in [plan_rub]: [', t.plan_rub, N']')

                        WHEN t.stock_start_pcs_clean IS NOT NULL
                             AND (t.stock_start_pcs_float IS NULL OR ABS(ROUND(t.stock_start_pcs_float, 2)) > 9999999999999999.99)
                            THEN CONCAT(N'RawId=', t.Id, N'. Invalid DECIMAL(18,2) value in [stock_start_pcs]: [', t.stock_start_pcs, N']')

                        WHEN t.stock_start_dd_clean IS NOT NULL
                             AND (t.stock_start_dd_float IS NULL OR ABS(ROUND(t.stock_start_dd_float, 2)) > 9999999999999999.99)
                            THEN CONCAT(N'RawId=', t.Id, N'. Invalid DECIMAL(18,2) value in [stock_start_dd]: [', t.stock_start_dd, N']')

                        WHEN t.sales_pcs_clean IS NOT NULL
                             AND (t.sales_pcs_float IS NULL OR ABS(ROUND(t.sales_pcs_float, 2)) > 9999999999999999.99)
                            THEN CONCAT(N'RawId=', t.Id, N'. Invalid DECIMAL(18,2) value in [sales_pcs]: [', t.sales_pcs, N']')

                        WHEN t.sales_rub_clean IS NOT NULL
                             AND (t.sales_rub_float IS NULL OR ABS(ROUND(t.sales_rub_float, 2)) > 9999999999999999.99)
                            THEN CONCAT(N'RawId=', t.Id, N'. Invalid DECIMAL(18,2) value in [sales_rub]: [', t.sales_rub, N']')

                        WHEN t.revenue_clean IS NOT NULL
                             AND (t.revenue_float IS NULL OR ABS(ROUND(t.revenue_float, 2)) > 9999999999999999.99)
                            THEN CONCAT(N'RawId=', t.Id, N'. Invalid DECIMAL(18,2) value in [revenue]: [', t.revenue, N']')

                        WHEN t.gp_clean IS NOT NULL
                             AND (t.gp_float IS NULL OR ABS(ROUND(t.gp_float, 2)) > 9999999999999999.99)
                            THEN CONCAT(N'RawId=', t.Id, N'. Invalid DECIMAL(18,2) value in [gp]: [', t.gp, N']')

                        WHEN t.cogs_clean IS NOT NULL
                             AND (t.cogs_float IS NULL OR ABS(ROUND(t.cogs_float, 2)) > 9999999999999999.99)
                            THEN CONCAT(N'RawId=', t.Id, N'. Invalid DECIMAL(18,2) value in [cogs]: [', t.cogs, N']')

                        WHEN t.sales_frp_price_clean IS NOT NULL
                             AND (t.sales_frp_price_float IS NULL OR ABS(ROUND(t.sales_frp_price_float, 2)) > 9999999999999999.99)
                            THEN CONCAT(N'RawId=', t.Id, N'. Invalid DECIMAL(18,2) value in [sales_frp_price]: [', t.sales_frp_price, N']')

                        WHEN t.sales_discount_clean IS NOT NULL
                             AND (t.sales_discount_float IS NULL OR ABS(ROUND(t.sales_discount_float, 2)) > 9999999999999999.99)
                            THEN CONCAT(N'RawId=', t.Id, N'. Invalid DECIMAL(18,2) value in [sales_discount]: [', t.sales_discount, N']')

                        WHEN t.stock_stores_pcs_clean IS NOT NULL
                             AND (t.stock_stores_pcs_float IS NULL OR ABS(ROUND(t.stock_stores_pcs_float, 2)) > 9999999999999999.99)
                            THEN CONCAT(N'RawId=', t.Id, N'. Invalid DECIMAL(18,2) value in [stock_stores_pcs]: [', t.stock_stores_pcs, N']')

                        WHEN t.stock_stores_dd_clean IS NOT NULL
                             AND (t.stock_stores_dd_float IS NULL OR ABS(ROUND(t.stock_stores_dd_float, 2)) > 9999999999999999.99)
                            THEN CONCAT(N'RawId=', t.Id, N'. Invalid DECIMAL(18,2) value in [stock_stores_dd]: [', t.stock_stores_dd, N']')

                        WHEN t.nazvanie_clean IS NOT NULL AND LEN(t.nazvanie_clean) > 255
                            THEN CONCAT(N'RawId=', t.Id, N'. Value in [nazvanie] exceeds target length 255: [', t.nazvanie, N']')

                        WHEN t.sales_channel_clean IS NOT NULL AND LEN(t.sales_channel_clean) > 255
                            THEN CONCAT(N'RawId=', t.Id, N'. Value in [sales_channel] exceeds target length 255: [', t.sales_channel, N']')

                        WHEN t.store_rus_clean IS NOT NULL AND LEN(t.store_rus_clean) > 255
                            THEN CONCAT(N'RawId=', t.Id, N'. Value in [store_rus] exceeds target length 255: [', t.store_rus, N']')

                        WHEN t.mfp_division_clean IS NOT NULL AND LEN(t.mfp_division_clean) > 255
                            THEN CONCAT(N'RawId=', t.Id, N'. Value in [mfp_division] exceeds target length 255: [', t.mfp_division, N']')

                        WHEN t.mfp_department_clean IS NOT NULL AND LEN(t.mfp_department_clean) > 255
                            THEN CONCAT(N'RawId=', t.Id, N'. Value in [mfp_department] exceeds target length 255: [', t.mfp_department, N']')

                        WHEN t.mfp_sub_department_clean IS NOT NULL AND LEN(t.mfp_sub_department_clean) > 255
                            THEN CONCAT(N'RawId=', t.Id, N'. Value in [mfp_sub_department] exceeds target length 255: [', t.mfp_sub_department, N']')

                        WHEN t.sku_brand_type_clean IS NOT NULL AND LEN(t.sku_brand_type_clean) > 255
                            THEN CONCAT(N'RawId=', t.Id, N'. Value in [sku_brand_type] exceeds target length 255: [', t.sku_brand_type, N']')

                        WHEN t.sku_tm_clean IS NOT NULL AND LEN(t.sku_tm_clean) > 255
                            THEN CONCAT(N'RawId=', t.Id, N'. Value in [sku_tm] exceeds target length 255: [', t.sku_tm, N']')

                        WHEN t.mfp_node_clean IS NOT NULL AND LEN(t.mfp_node_clean) > 255
                            THEN CONCAT(N'RawId=', t.Id, N'. Value in [mfp_node] exceeds target length 255: [', t.mfp_node, N']')

                        WHEN t.section_clean IS NOT NULL AND LEN(t.section_clean) > 255
                            THEN CONCAT(N'RawId=', t.Id, N'. Value in [section] exceeds target length 255: [', t.section, N']')

                        WHEN t.merchandise_sub_group_clean IS NOT NULL AND LEN(t.merchandise_sub_group_clean) > 255
                            THEN CONCAT(N'RawId=', t.Id, N'. Value in [merchandise_sub_group] exceeds target length 255: [', t.merchandise_sub_group, N']')

                        WHEN t.campaign_sales_clean IS NOT NULL AND LEN(t.campaign_sales_clean) > 255
                            THEN CONCAT(N'RawId=', t.Id, N'. Value in [campaign_sales] exceeds target length 255: [', t.campaign_sales, N']')

                        WHEN t.sku_phase_clean IS NOT NULL AND LEN(t.sku_phase_clean) > 255
                            THEN CONCAT(N'RawId=', t.Id, N'. Value in [sku_phase] exceeds target length 255: [', t.sku_phase, N']')

                        WHEN t.draivery_cd_clean IS NOT NULL AND LEN(t.draivery_cd_clean) > 255
                            THEN CONCAT(N'RawId=', t.Id, N'. Value in [draivery_cd] exceeds target length 255: [', t.draivery_cd, N']')

                        WHEN t.sku_color_rus_clean IS NOT NULL AND LEN(t.sku_color_rus_clean) > 255
                            THEN CONCAT(N'RawId=', t.Id, N'. Value in [sku_color_rus] exceeds target length 255: [', t.sku_color_rus, N']')

                        WHEN t.sku_composition_clean IS NOT NULL AND LEN(t.sku_composition_clean) > 255
                            THEN CONCAT(N'RawId=', t.Id, N'. Value in [sku_composition] exceeds target length 255: [', t.sku_composition, N']')

                        WHEN t.sku_supplier_clean IS NOT NULL AND LEN(t.sku_supplier_clean) > 255
                            THEN CONCAT(N'RawId=', t.Id, N'. Value in [sku_supplier] exceeds target length 255: [', t.sku_supplier, N']')

                        WHEN t.sku_name_clean IS NOT NULL AND LEN(t.sku_name_clean) > 255
                            THEN CONCAT(N'RawId=', t.Id, N'. Value in [sku_name] exceeds target length 255: [', t.sku_name, N']')

                        WHEN t.sku_collection_clean IS NOT NULL AND LEN(t.sku_collection_clean) > 255
                            THEN CONCAT(N'RawId=', t.Id, N'. Value in [sku_collection] exceeds target length 255: [', t.sku_collection, N']')

                        WHEN t.sku_comment_clean IS NOT NULL AND LEN(t.sku_comment_clean) > 255
                            THEN CONCAT(N'RawId=', t.Id, N'. Value in [sku_comment] exceeds target length 255: [', t.sku_comment, N']')

                        ELSE NULL
                    END
            FROM Typed t
        )
         INSERT INTO dbo.DWH_Excel_Load_Error
         (
             LoadSessionId,
             LoadTypeCode,
             ErrorLayer,
             ExcelRowNum,
             RawId,
             FieldName,
             ErrorCode,
             ErrorReason,
             ErrorMessage
         )
         SELECT
             @LoadSessionId,
             @LoadTypeCode,
             N'VALIDATION',
             v.ExcelRowNum,
             v.Id,
             v.FieldName,
             N'INVALID_VALUE',
             v.ErrorMessage,
             LEFT(v.ErrorMessage, 4000)
         FROM Validation v
         WHERE v.ErrorMessage IS NOT NULL;

        /* 5. Подсчёт ошибок */
        SELECT
                @ErrorRows = COUNT_BIG(*)
        FROM dbo.DWH_Excel_Load_Error
        WHERE LoadSessionId = @LoadSessionId
          AND LoadTypeCode = @LoadTypeCode;

        /* 6. Если есть ошибки — ничего не переносим */
        IF @ErrorRows > 0
            BEGIN
                SET @Success = 0;
                SET @LoadedRows = 0;
                SET @Message = CONCAT(
                        N'Validation failed. Total raw rows: ', @TotalRows,
                        N'. Error rows: ', @ErrorRows,
                        N'. Nothing was loaded into dbo.CD_data.'
                    );

                SELECT
                    @LoadSessionId AS LoadSessionId,
                    @Success AS Success,
                    @TotalRows AS TotalRows,
                    @LoadedRows AS LoadedRows,
                    @ErrorRows AS ErrorRows,
                    @Message AS Message;

                RETURN;
            END;

        /* 7. Ошибок нет — переносим всё в target */
        BEGIN TRANSACTION;

        ;WITH Src AS
                  (
                      SELECT
                          r.LoadSessionId,

                          nazvanie_clean = NULLIF(LTRIM(RTRIM(r.nazvanie)), N''),
                          sales_channel_clean = NULLIF(LTRIM(RTRIM(r.sales_channel)), N''),
                          store_rus_clean = NULLIF(LTRIM(RTRIM(r.store_rus)), N''),
                          mfp_division_clean = NULLIF(LTRIM(RTRIM(r.mfp_division)), N''),
                          mfp_department_clean = NULLIF(LTRIM(RTRIM(r.mfp_department)), N''),
                          mfp_sub_department_clean = NULLIF(LTRIM(RTRIM(r.mfp_sub_department)), N''),
                          sku_brand_type_clean = NULLIF(LTRIM(RTRIM(r.sku_brand_type)), N''),
                          sku_tm_clean = NULLIF(LTRIM(RTRIM(r.sku_tm)), N''),
                          mfp_node_clean = NULLIF(LTRIM(RTRIM(r.mfp_node)), N''),
                          section_clean = NULLIF(LTRIM(RTRIM(r.section)), N''),
                          merchandise_sub_group_clean = NULLIF(LTRIM(RTRIM(r.merchandise_sub_group)), N''),
                          campaign_sales_clean = NULLIF(LTRIM(RTRIM(r.campaign_sales)), N''),
                          sku_phase_clean = NULLIF(LTRIM(RTRIM(r.sku_phase)), N''),
                          draivery_cd_clean = NULLIF(LTRIM(RTRIM(r.draivery_cd)), N''),
                          sku_color_rus_clean = NULLIF(LTRIM(RTRIM(r.sku_color_rus)), N''),
                          sku_composition_clean = NULLIF(LTRIM(RTRIM(r.sku_composition)), N''),
                          sku_supplier_clean = NULLIF(LTRIM(RTRIM(r.sku_supplier)), N''),
                          sku_name_clean = NULLIF(LTRIM(RTRIM(r.sku_name)), N''),
                          sku_collection_clean = NULLIF(LTRIM(RTRIM(r.sku_collection)), N''),
                          sku_comment_clean = NULLIF(LTRIM(RTRIM(r.sku_comment)), N''),

                          god_clean = NULLIF(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.god)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N''),
                          sezon_clean = NULLIF(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sezon)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N''),
                          den_clean = NULLIF(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.den)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N''),
                          sku_style_color_clean = NULLIF(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sku_style_color)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N''),
                          plan_rub_clean = NULLIF(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.plan_rub)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N''),

                          data_clean = NULLIF(LTRIM(RTRIM(r.data)), N''),

                          stock_start_pcs_clean = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.stock_start_pcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'), N''),
                          stock_start_dd_clean = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.stock_start_dd)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'), N''),
                          sales_pcs_clean = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sales_pcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'), N''),
                          sales_rub_clean = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sales_rub)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'), N''),
                          revenue_clean = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.revenue)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'), N''),
                          gp_clean = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.gp)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'), N''),
                          cogs_clean = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.cogs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'), N''),
                          sales_frp_price_clean = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sales_frp_price)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'), N''),
                          sales_discount_clean = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sales_discount)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'), N''),
                          stock_stores_pcs_clean = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.stock_stores_pcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'), N''),
                          stock_stores_dd_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(
                                                  REPLACE(
                                                          REPLACE(LTRIM(RTRIM(r.stock_stores_dd)), NCHAR(160), N''),
                                                          NCHAR(8239), N''
                                                      ),
                                                  N' ', N''
                                              ),
                                          N',', N'.'
                                      ),
                                  N''
                              )
                                                         FROM dbo.CD_data_raw r
                                                         WHERE r.LoadSessionId = @LoadSessionId
                              ),
                          Typed AS
        (
            SELECT
                s.*,

                god_float = TRY_CONVERT(FLOAT, s.god_clean),
                sezon_float = TRY_CONVERT(FLOAT, s.sezon_clean),
                den_float = TRY_CONVERT(FLOAT, s.den_clean),
                sku_style_color_float = TRY_CONVERT(FLOAT, s.sku_style_color_clean),
                plan_rub_float = TRY_CONVERT(FLOAT, s.plan_rub_clean),

                stock_start_pcs_float = TRY_CONVERT(FLOAT, s.stock_start_pcs_clean),
                stock_start_dd_float = TRY_CONVERT(FLOAT, s.stock_start_dd_clean),
                sales_pcs_float = TRY_CONVERT(FLOAT, s.sales_pcs_clean),
                sales_rub_float = TRY_CONVERT(FLOAT, s.sales_rub_clean),
                revenue_float = TRY_CONVERT(FLOAT, s.revenue_clean),
                gp_float = TRY_CONVERT(FLOAT, s.gp_clean),
                cogs_float = TRY_CONVERT(FLOAT, s.cogs_clean),
                sales_frp_price_float = TRY_CONVERT(FLOAT, s.sales_frp_price_clean),
                sales_discount_float = TRY_CONVERT(FLOAT, s.sales_discount_clean),
                stock_stores_pcs_float = TRY_CONVERT(FLOAT, s.stock_stores_pcs_clean),
                stock_stores_dd_float = TRY_CONVERT(FLOAT, s.stock_stores_dd_clean),

                data_value =
                    COALESCE(
                        TRY_CONVERT(DATE, s.data_clean, 104),
                        TRY_CONVERT(DATE, s.data_clean, 103),
                        TRY_CONVERT(DATE, s.data_clean, 23),
                        TRY_CONVERT(DATE, s.data_clean, 120),
                        TRY_CONVERT(DATE, s.data_clean, 1),
                        CASE
                            WHEN TRY_CONVERT(INT, s.data_clean) IS NOT NULL
                                 AND TRY_CONVERT(INT, s.data_clean) BETWEEN 1 AND 60000
                            THEN DATEADD(DAY, TRY_CONVERT(INT, s.data_clean) - 2, CONVERT(DATE, '19000101', 112))
                            ELSE NULL
                        END
                    )
            FROM Src s
        )
         INSERT INTO dbo.CD_data
         (
             LoadSessionId,
             nazvanie,
             god,
             sezon,
             den,
             data,
             sales_channel,
             store_rus,
             mfp_division,
             mfp_department,
             mfp_sub_department,
             sku_brand_type,
             sku_tm,
             mfp_node,
             section,
             merchandise_sub_group,
             campaign_sales,
             sku_style_color,
             sku_phase,
             stock_start_pcs,
             stock_start_dd,
             sales_pcs,
             sales_rub,
             revenue,
             gp,
             cogs,
             sales_frp_price,
             sales_discount,
             stock_stores_pcs,
             stock_stores_dd,
             plan_rub,
             draivery_cd,
             sku_color_rus,
             sku_composition,
             sku_supplier,
             sku_name,
             sku_collection,
             sku_comment
         )
         SELECT
             t.LoadSessionId,
             t.nazvanie_clean,
             CONVERT(INT, ABS(t.god_float)),
             CONVERT(INT, ABS(t.sezon_float)),
             CONVERT(INT, ABS(t.den_float)),
             t.data_value,
             t.sales_channel_clean,
             t.store_rus_clean,
             t.mfp_division_clean,
             t.mfp_department_clean,
             t.mfp_sub_department_clean,
             t.sku_brand_type_clean,
             t.sku_tm_clean,
             t.mfp_node_clean,
             t.section_clean,
             t.merchandise_sub_group_clean,
             t.campaign_sales_clean,
             CONVERT(INT, ABS(t.sku_style_color_float)),
             t.sku_phase_clean,
             CONVERT(DECIMAL(18,2), ABS(ROUND(t.stock_start_pcs_float, 2))),
             CONVERT(DECIMAL(18,2), ABS(ROUND(t.stock_start_dd_float, 2))),
             CONVERT(DECIMAL(18,2), ABS(ROUND(t.sales_pcs_float, 2))),
             CONVERT(DECIMAL(18,2), ABS(ROUND(t.sales_rub_float, 2))),
             CONVERT(DECIMAL(18,2), ABS(ROUND(t.revenue_float, 2))),
             CONVERT(DECIMAL(18,2), ABS(ROUND(t.gp_float, 2))),
             CONVERT(DECIMAL(18,2), ABS(ROUND(t.cogs_float, 2))),
             CONVERT(DECIMAL(18,2), ABS(ROUND(t.sales_frp_price_float, 2))),
             CONVERT(DECIMAL(18,2), ABS(ROUND(t.sales_discount_float, 2))),
             CONVERT(DECIMAL(18,2), ABS(ROUND(t.stock_stores_pcs_float, 2))),
             CONVERT(DECIMAL(18,2), ABS(ROUND(t.stock_stores_dd_float, 2))),
             CONVERT(INT, ABS(t.plan_rub_float)),
             t.draivery_cd_clean,
             t.sku_color_rus_clean,
             t.sku_composition_clean,
             t.sku_supplier_clean,
             t.sku_name_clean,
             t.sku_collection_clean,
             t.sku_comment_clean
         FROM Typed t;

        SET @LoadedRows = @@ROWCOUNT;

        COMMIT TRANSACTION;

        SET @Success = 1;
        SET @Message = CONCAT(
                N'Success. Total raw rows: ', @TotalRows,
                N'. Loaded rows: ', @LoadedRows,
                N'. Error rows: 0.'
            );

        SELECT
            @LoadSessionId AS LoadSessionId,
            @Success AS Success,
            @TotalRows AS TotalRows,
            @LoadedRows AS LoadedRows,
            CAST(0 AS BIGINT) AS ErrorRows,
            @Message AS Message;

    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;

        DECLARE @CatchMessage NVARCHAR(4000) = ERROR_MESSAGE();

        BEGIN TRY
            INSERT INTO dbo.DWH_Excel_Load_Error
            (
                LoadSessionId,
                LoadTypeCode,
                ErrorLayer,
                ExcelRowNum,
                RawId,
                FieldName,
                ErrorCode,
                ErrorReason,
                ErrorMessage
            )
            VALUES
                (
                    @LoadSessionId,
                    @LoadTypeCode,
                    N'PROCESSING',
                    NULL,
                    0,
                    NULL,
                    N'UNEXPECTED_PROCESSING_ERROR',
                    LEFT(CONCAT(N'Unexpected processing error: ', @CatchMessage), 4000),
                    LEFT(CONCAT(N'Unexpected processing error: ', @CatchMessage), 4000)
                );
        END TRY
        BEGIN CATCH
            /* ничего не делаем, если даже запись ошибки не удалась */
        END CATCH;

        SELECT
                @ErrorRows = COUNT_BIG(*)
        FROM dbo.DWH_Excel_Load_Error
        WHERE LoadSessionId = @LoadSessionId
          AND LoadTypeCode = @LoadTypeCode;

        SET @Message = LEFT(CONCAT(N'Processing failed: ', @CatchMessage), 2000);

        SELECT
            @LoadSessionId AS LoadSessionId,
            CAST(0 AS BIT) AS Success,
            @TotalRows AS TotalRows,
            CAST(0 AS BIGINT) AS LoadedRows,
            @ErrorRows AS ErrorRows,
            @Message AS Message;
    END CATCH
END;
GO