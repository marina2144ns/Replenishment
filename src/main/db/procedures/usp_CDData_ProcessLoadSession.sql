USE ReplenishmentDWH;
GO

CREATE OR ALTER PROCEDURE dbo.usp_CDData_ProcessLoadSession
@LoadSessionId BIGINT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE
        @TotalRows  BIGINT = 0,
        @LoadedRows BIGINT = 0,
        @ErrorRows  BIGINT = 0,
        @Success    BIT    = 0,
        @Message    NVARCHAR(2000);

    BEGIN TRY
        /* 1. Проверка существования сессии */
        IF NOT EXISTS
            (
                SELECT 1
                FROM dbo.CD_data_Load_session
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
        DELETE FROM dbo.CD_data_Load_error
        WHERE LoadSessionId = @LoadSessionId;

        DELETE FROM dbo.CD_data
        WHERE LoadSessionId = @LoadSessionId;

        /* 3. Подсчёт raw-строк */
        SELECT
                @TotalRows = COUNT_BIG(*)
        FROM dbo.CD_data_raw
        WHERE LoadSessionId = @LoadSessionId;

        /* 4. Валидация: одна ошибка на одну строку raw */
        ;WITH Src AS
                  (
                      SELECT
                          r.Id,
                          r.LoadSessionId,

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
                          god_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.god)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          sezon_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sezon)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          den_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.den)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          data_clean = NULLIF(LTRIM(RTRIM(r.data)), N''),

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

                          sku_style_color_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sku_style_color)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),

                          stock_start_pcs_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.stock_start_pcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          stock_start_dd_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.stock_start_dd)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          sales_pcs_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sales_pcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          sales_rub_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sales_rub)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          revenue_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.revenue)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          gp_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.gp)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          cogs_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.cogs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          sales_frp_price_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sales_frp_price)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          sales_discount_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sales_discount)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          stock_stores_pcs_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.stock_stores_pcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          stock_stores_dd_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.stock_stores_dd)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          plan_rub_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.plan_rub)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              )
                      FROM dbo.CD_data_raw r
                      WHERE r.LoadSessionId = @LoadSessionId
                  ),
              Validation AS
                  (
                      SELECT
                          s.Id,
                          ErrorMessage =
                              CASE
                                  /* INT / DATETIME / DECIMAL */
                                  WHEN s.god_clean IS NOT NULL
                                      AND TRY_CONVERT(INT, s.god_clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid INT value in [god]: [', s.god, N']')

                                  WHEN s.sezon_clean IS NOT NULL
                                      AND TRY_CONVERT(INT, s.sezon_clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid INT value in [sezon]: [', s.sezon, N']')

                                  WHEN s.den_clean IS NOT NULL
                                      AND TRY_CONVERT(INT, s.den_clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid INT value in [den]: [', s.den, N']')

                                  WHEN s.data_clean IS NOT NULL
                                      AND TRY_CONVERT(DATE, s.data_clean, 1) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid DATE value in [data]: [', s.data, N']')

                                  WHEN s.sku_style_color_clean IS NOT NULL
                                      AND TRY_CONVERT(INT, s.sku_style_color_clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid INT value in [sku_style_color]: [', s.sku_style_color, N']')

                                  WHEN s.stock_start_pcs_clean IS NOT NULL
                                      AND TRY_CONVERT(DECIMAL(18,2), s.stock_start_pcs_clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid DECIMAL(18,2) value in [stock_start_pcs]: [', s.stock_start_pcs, N']')

                                  WHEN s.stock_start_dd_clean IS NOT NULL
                                      AND TRY_CONVERT(DECIMAL(18,2), s.stock_start_dd_clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid DECIMAL(18,2) value in [stock_start_dd]: [', s.stock_start_dd, N']')

                                  WHEN s.sales_pcs_clean IS NOT NULL
                                      AND TRY_CONVERT(DECIMAL(18,2), s.sales_pcs_clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid DECIMAL(18,2) value in [sales_pcs]: [', s.sales_pcs, N']')

                                  WHEN s.sales_rub_clean IS NOT NULL
                                      AND TRY_CONVERT(DECIMAL(18,2), s.sales_rub_clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid DECIMAL(18,2) value in [sales_rub]: [', s.sales_rub, N']')

                                  WHEN s.revenue_clean IS NOT NULL
                                      AND TRY_CONVERT(DECIMAL(18,2), s.revenue_clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid DECIMAL(18,2) value in [revenue]: [', s.revenue, N']')

                                  WHEN s.gp_clean IS NOT NULL
                                      AND TRY_CONVERT(DECIMAL(18,2), s.gp_clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid DECIMAL(18,2) value in [gp]: [', s.gp, N']')

                                  WHEN s.cogs_clean IS NOT NULL
                                      AND TRY_CONVERT(FLOAT, s.cogs_clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid DECIMAL(18,2) value in [cogs]: [', s.cogs, N']')

                                  WHEN s.sales_frp_price_clean IS NOT NULL
                                      AND TRY_CONVERT(DECIMAL(18,2), s.sales_frp_price_clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid DECIMAL(18,2) value in [sales_frp_price]: [', s.sales_frp_price, N']')

                                  WHEN s.sales_discount_clean IS NOT NULL
                                      AND TRY_CONVERT(DECIMAL(18,2), s.sales_discount_clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid DECIMAL(18,2) value in [sales_discount]: [', s.sales_discount, N']')

                                  WHEN s.stock_stores_pcs_clean IS NOT NULL
                                      AND TRY_CONVERT(DECIMAL(18,2), s.stock_stores_pcs_clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid DECIMAL(18,2) value in [stock_stores_pcs]: [', s.stock_stores_pcs, N']')

                                  WHEN s.stock_stores_dd_clean IS NOT NULL
                                      AND TRY_CONVERT(DECIMAL(18,2), s.stock_stores_dd_clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid DECIMAL(18,2) value in [stock_stores_dd]: [', s.stock_stores_dd, N']')

                                  WHEN s.plan_rub_clean IS NOT NULL
                                      AND TRY_CONVERT(INT, s.plan_rub_clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid INT value in [plan_rub]: [', s.plan_rub, N']')

                                  /* NVARCHAR(255) */
                                  WHEN s.nazvanie_clean IS NOT NULL
                                      AND LEN(s.nazvanie_clean) > 255 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [nazvanie] exceeds target length 255: [', s.nazvanie, N']')

                                  WHEN s.sales_channel_clean IS NOT NULL
                                      AND LEN(s.sales_channel_clean) > 255 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [sales_channel] exceeds target length 255: [', s.sales_channel, N']')

                                  WHEN s.store_rus_clean IS NOT NULL
                                      AND LEN(s.store_rus_clean) > 255 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [store_rus] exceeds target length 255: [', s.store_rus, N']')

                                  WHEN s.mfp_division_clean IS NOT NULL
                                      AND LEN(s.mfp_division_clean) > 255 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [mfp_division] exceeds target length 255: [', s.mfp_division, N']')

                                  WHEN s.mfp_department_clean IS NOT NULL
                                      AND LEN(s.mfp_department_clean) > 255 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [mfp_department] exceeds target length 255: [', s.mfp_department, N']')

                                  WHEN s.mfp_sub_department_clean IS NOT NULL
                                      AND LEN(s.mfp_sub_department_clean) > 255 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [mfp_sub_department] exceeds target length 255: [', s.mfp_sub_department, N']')

                                  WHEN s.sku_brand_type_clean IS NOT NULL
                                      AND LEN(s.sku_brand_type_clean) > 255 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [sku_brand_type] exceeds target length 255: [', s.sku_brand_type, N']')

                                  WHEN s.sku_tm_clean IS NOT NULL
                                      AND LEN(s.sku_tm_clean) > 255 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [sku_tm] exceeds target length 255: [', s.sku_tm, N']')

                                  WHEN s.mfp_node_clean IS NOT NULL
                                      AND LEN(s.mfp_node_clean) > 255 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [mfp_node] exceeds target length 255: [', s.mfp_node, N']')

                                  WHEN s.section_clean IS NOT NULL
                                      AND LEN(s.section_clean) > 255 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [section] exceeds target length 255: [', s.section, N']')

                                  WHEN s.merchandise_sub_group_clean IS NOT NULL
                                      AND LEN(s.merchandise_sub_group_clean) > 255 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [merchandise_sub_group] exceeds target length 255: [', s.merchandise_sub_group, N']')

                                  WHEN s.campaign_sales_clean IS NOT NULL
                                      AND LEN(s.campaign_sales_clean) > 255 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [campaign_sales] exceeds target length 255: [', s.campaign_sales, N']')

                                  WHEN s.sku_phase_clean IS NOT NULL
                                      AND LEN(s.sku_phase_clean) > 255 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [sku_phase] exceeds target length 255: [', s.sku_phase, N']')

                                  WHEN s.draivery_cd_clean IS NOT NULL
                                      AND LEN(s.draivery_cd_clean) > 255 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [draivery_cd] exceeds target length 255: [', s.draivery_cd, N']')

                                  WHEN s.sku_color_rus_clean IS NOT NULL
                                      AND LEN(s.sku_color_rus_clean) > 255 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [sku_color_rus] exceeds target length 255: [', s.sku_color_rus, N']')

                                  WHEN s.sku_composition_clean IS NOT NULL
                                      AND LEN(s.sku_composition_clean) > 255 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [sku_composition] exceeds target length 255: [', s.sku_composition, N']')

                                  WHEN s.sku_supplier_clean IS NOT NULL
                                      AND LEN(s.sku_supplier_clean) > 255 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [sku_supplier] exceeds target length 255: [', s.sku_supplier, N']')

                                  WHEN s.sku_name_clean IS NOT NULL
                                      AND LEN(s.sku_name_clean) > 255 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [sku_name] exceeds target length 255: [', s.sku_name, N']')

                                  WHEN s.sku_collection_clean IS NOT NULL
                                      AND LEN(s.sku_collection_clean) > 255 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [sku_collection] exceeds target length 255: [', s.sku_collection, N']')

                                  WHEN s.sku_comment_clean IS NOT NULL
                                      AND LEN(s.sku_comment_clean) > 255 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [sku_comment] exceeds target length 255: [', s.sku_comment, N']')

                                  ELSE NULL
                                  END
                      FROM Src s
                  )
         INSERT INTO dbo.CD_data_Load_error
         (
             LoadSessionId,
             RawId,
             Stage,
             ErrorMessage
         )
         SELECT
             @LoadSessionId,
             v.Id,
             'VALIDATION',
             v.ErrorMessage
         FROM Validation v
         WHERE v.ErrorMessage IS NOT NULL;

        /* 5. Подсчёт ошибок */
        SELECT
                @ErrorRows = COUNT_BIG(*)
        FROM dbo.CD_data_Load_error
        WHERE LoadSessionId = @LoadSessionId;

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

                          god_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.god)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          sezon_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sezon)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          den_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.den)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          data_clean = NULLIF(LTRIM(RTRIM(r.data)), N''),

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

                          sku_style_color_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sku_style_color)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),

                          stock_start_pcs_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.stock_start_pcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          stock_start_dd_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.stock_start_dd)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          sales_pcs_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sales_pcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          sales_rub_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sales_rub)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          revenue_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.revenue)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          gp_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.gp)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          cogs_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.cogs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          sales_frp_price_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sales_frp_price)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          sales_discount_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.sales_discount)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          stock_stores_pcs_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.stock_stores_pcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          stock_stores_dd_clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.stock_stores_dd)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),
                          plan_rub_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.plan_rub)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              )
                      FROM dbo.CD_data_raw r
                      WHERE r.LoadSessionId = @LoadSessionId
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
             s.LoadSessionId,
             s.nazvanie_clean,
             TRY_CONVERT(INT, s.god_clean),
             TRY_CONVERT(INT, s.sezon_clean),
             TRY_CONVERT(INT, s.den_clean),
             TRY_CONVERT(DATE, s.data_clean,1),
             s.sales_channel_clean,
             s.store_rus_clean,
             s.mfp_division_clean,
             s.mfp_department_clean,
             s.mfp_sub_department_clean,
             s.sku_brand_type_clean,
             s.sku_tm_clean,
             s.mfp_node_clean,
             s.section_clean,
             s.merchandise_sub_group_clean,
             s.campaign_sales_clean,
             TRY_CONVERT(INT, s.sku_style_color_clean),
             s.sku_phase_clean,
             TRY_CONVERT(DECIMAL(18,2), s.stock_start_pcs_clean),
             TRY_CONVERT(DECIMAL(18,2), s.stock_start_dd_clean),
             TRY_CONVERT(DECIMAL(18,2), s.sales_pcs_clean),
             TRY_CONVERT(DECIMAL(18,2), s.sales_rub_clean),
             TRY_CONVERT(DECIMAL(18,2), s.revenue_clean),
             TRY_CONVERT(DECIMAL(18,2), s.gp_clean),
             TRY_CONVERT(DECIMAL(18,2), s.cogs_clean),
             TRY_CONVERT(DECIMAL(18,2), s.sales_frp_price_clean),
             TRY_CONVERT(DECIMAL(18,2), s.sales_discount_clean),
             TRY_CONVERT(DECIMAL(18,2), s.stock_stores_pcs_clean),
             TRY_CONVERT(DECIMAL(18,2), s.stock_stores_dd_clean),
             TRY_CONVERT(INT, s.plan_rub_clean),
             s.draivery_cd_clean,
             s.sku_color_rus_clean,
             s.sku_composition_clean,
             s.sku_supplier_clean,
             s.sku_name_clean,
             s.sku_collection_clean,
             s.sku_comment_clean
         FROM Src s;

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
            INSERT INTO dbo.CD_data_Load_error
            (
                LoadSessionId,
                RawId,
                Stage,
                ErrorMessage
            )
            VALUES
                (
                    @LoadSessionId,
                    0,
                    'PROCESSING',
                    LEFT(CONCAT(N'Unexpected processing error: ', @CatchMessage), 4000)
                );
        END TRY
        BEGIN CATCH
            /* ничего не делаем, если даже запись ошибки не удалась */
        END CATCH;

        SELECT
                @ErrorRows = COUNT_BIG(*)
        FROM dbo.CD_data_Load_error
        WHERE LoadSessionId = @LoadSessionId;

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