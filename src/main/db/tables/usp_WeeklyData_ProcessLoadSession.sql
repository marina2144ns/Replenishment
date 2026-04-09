USE ReplenishmentDWH;
GO

CREATE PROCEDURE dbo.usp_WeeklyData_ProcessLoadSession
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
                FROM dbo.Weekly_data_Load_session
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
        DELETE FROM dbo.Weekly_data_Load_error
        WHERE LoadSessionId = @LoadSessionId;

        DELETE FROM dbo.Weekly_data
        WHERE LoadSessionId = @LoadSessionId;

        /* 3. Подсчёт raw-строк */
        SELECT
                @TotalRows = COUNT_BIG(*)
        FROM dbo.Weekly_data_raw
        WHERE LoadSessionId = @LoadSessionId;

        /* 4. Валидация: одна ошибка на одну строку raw */
        ;WITH Src AS
                  (
                      SELECT
                          r.Id,
                          r.LoadSessionId,
                          r.Year21,
                          r.Week21,
                          r.YearCorr,
                          r.WeekCorr,
                          r.[Year],
                          r.[Week],
                          r.SalesChannelBpo,
                          r.StoreRusBpo,
                          r.StoreRus,
                          r.MfpDivisionNew,
                          r.MfpDepartment,
                          r.SkuSeasonBudget,
                          r.TypeOfSales,
                          r.TotalStockPcs,
                          r.TotalStockDdp,
                          r.SalesPcs,
                          r.SalesRub,
                          r.Revenue,
                          r.Gp,
                          r.DiscountTotalRub,
                          r.MfpDivision,
                          r.Season,
                          r.[Month],
                          r.Bundle,
                          r.Seasonality,

                          Year21_Clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.Year21)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          Week21_Clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.Week21)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          YearCorr_Clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.YearCorr)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          WeekCorr_Clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.WeekCorr)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          Year_Clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.[Year])), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          Week_Clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.[Week])), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),

                          SalesChannelBpo_Clean = NULLIF(LTRIM(RTRIM(r.SalesChannelBpo)), N''),
                          StoreRusBpo_Clean = NULLIF(LTRIM(RTRIM(r.StoreRusBpo)), N''),
                          StoreRus_Clean = NULLIF(LTRIM(RTRIM(r.StoreRus)), N''),
                          MfpDivisionNew_Clean = NULLIF(LTRIM(RTRIM(r.MfpDivisionNew)), N''),
                          MfpDepartment_Clean = NULLIF(LTRIM(RTRIM(r.MfpDepartment)), N''),
                          SkuSeasonBudget_Clean = NULLIF(LTRIM(RTRIM(r.SkuSeasonBudget)), N''),
                          TypeOfSales_Clean = NULLIF(LTRIM(RTRIM(r.TypeOfSales)), N''),
                          MfpDivision_Clean = NULLIF(LTRIM(RTRIM(r.MfpDivision)), N''),
                          Season_Clean = NULLIF(LTRIM(RTRIM(r.Season)), N''),
                          Month_Clean = NULLIF(LTRIM(RTRIM(r.[Month])), N''),
                          Bundle_Clean = NULLIF(LTRIM(RTRIM(r.Bundle)), N''),
                          Seasonality_Clean = NULLIF(LTRIM(RTRIM(r.Seasonality)), N''),

                          TotalStockPcs_Clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.TotalStockPcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),

                          TotalStockDdp_Clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.TotalStockDdp)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),

                          SalesPcs_Clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.SalesPcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),

                          SalesRub_Clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.SalesRub)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),

                          Revenue_Clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.Revenue)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),

                          Gp_Clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.Gp)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),

                          DiscountTotalRub_Clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.DiscountTotalRub)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              )
                      FROM dbo.Weekly_data_raw r
                      WHERE r.LoadSessionId = @LoadSessionId
                  ),
              Validation AS
                  (
                      SELECT
                          s.Id,
                          ErrorMessage =
                              CASE
                                  WHEN s.Year_Clean IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Required field [Year] is empty.')

                                  WHEN s.Week_Clean IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Required field [Week] is empty.')

                                  WHEN s.Year21_Clean IS NOT NULL
                                      AND TRY_CONVERT(SMALLINT, s.Year21_Clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid SMALLINT value in [Year21]: [', s.Year21, N']')

                                  WHEN s.Week21_Clean IS NOT NULL
                                      AND TRY_CONVERT(SMALLINT, s.Week21_Clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid SMALLINT value in [Week21]: [', s.Week21, N']')

                                  WHEN s.YearCorr_Clean IS NOT NULL
                                      AND TRY_CONVERT(SMALLINT, s.YearCorr_Clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid SMALLINT value in [YearCorr]: [', s.YearCorr, N']')

                                  WHEN s.WeekCorr_Clean IS NOT NULL
                                      AND TRY_CONVERT(SMALLINT, s.WeekCorr_Clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid SMALLINT value in [WeekCorr]: [', s.WeekCorr, N']')

                                  WHEN TRY_CONVERT(SMALLINT, s.Year_Clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid SMALLINT value in [Year]: [', COALESCE(s.[Year], N'NULL'), N']')

                                  WHEN TRY_CONVERT(SMALLINT, s.Week_Clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid SMALLINT value in [Week]: [', COALESCE(s.[Week], N'NULL'), N']')

                                  WHEN TRY_CONVERT(SMALLINT, s.Week21_Clean) IS NOT NULL
                                      AND TRY_CONVERT(SMALLINT, s.Week21_Clean) NOT BETWEEN 1 AND 100 THEN
                                      CONCAT(N'RawId=', s.Id, N'. [Week21] is out of range 1..100: [', s.Week21, N']')

                                  WHEN TRY_CONVERT(SMALLINT, s.WeekCorr_Clean) IS NOT NULL
                                      AND TRY_CONVERT(SMALLINT, s.WeekCorr_Clean) NOT BETWEEN 1 AND 100 THEN
                                      CONCAT(N'RawId=', s.Id, N'. [WeekCorr] is out of range 1..100: [', s.WeekCorr, N']')

                                  WHEN TRY_CONVERT(SMALLINT, s.Week_Clean) IS NOT NULL
                                      AND TRY_CONVERT(SMALLINT, s.Week_Clean) NOT BETWEEN 1 AND 100 THEN
                                      CONCAT(N'RawId=', s.Id, N'. [Week] is out of range 1..100: [', s.[Week], N']')

                                  WHEN s.TotalStockPcs_Clean IS NOT NULL
                                      AND TRY_CONVERT(INT, s.TotalStockPcs_Clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid INT value in [TotalStockPcs]: [', s.TotalStockPcs, N']')

                                  WHEN s.TotalStockDdp_Clean IS NOT NULL
                                      AND TRY_CONVERT(DECIMAL(18,2), s.TotalStockDdp_Clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid DECIMAL(18,2) value in [TotalStockDdp]: [', s.TotalStockDdp, N']')

                                  WHEN s.SalesPcs_Clean IS NOT NULL
                                      AND TRY_CONVERT(INT, s.SalesPcs_Clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid INT value in [SalesPcs]: [', s.SalesPcs, N']')

                                  WHEN s.SalesRub_Clean IS NOT NULL
                                      AND TRY_CONVERT(DECIMAL(18,2), s.SalesRub_Clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid DECIMAL(18,2) value in [SalesRub]: [', s.SalesRub, N']')

                                  WHEN s.Revenue_Clean IS NOT NULL
                                      AND TRY_CONVERT(DECIMAL(18,2), s.Revenue_Clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid DECIMAL(18,2) value in [Revenue]: [', s.Revenue, N']')

                                  WHEN s.Gp_Clean IS NOT NULL
                                      AND TRY_CONVERT(DECIMAL(18,2), s.Gp_Clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid DECIMAL(18,2) value in [Gp]: [', s.Gp, N']')

                                  WHEN s.DiscountTotalRub_Clean IS NOT NULL
                                      AND TRY_CONVERT(DECIMAL(18,2), s.DiscountTotalRub_Clean) IS NULL THEN
                                      CONCAT(N'RawId=', s.Id, N'. Invalid DECIMAL(18,2) value in [DiscountTotalRub]: [', s.DiscountTotalRub, N']')

                                  WHEN s.SalesChannelBpo_Clean IS NOT NULL
                                      AND LEN(s.SalesChannelBpo_Clean) > 50 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [SalesChannelBpo] exceeds target length 50: [', s.SalesChannelBpo, N']')

                                  WHEN s.StoreRusBpo_Clean IS NOT NULL
                                      AND LEN(s.StoreRusBpo_Clean) > 50 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [StoreRusBpo] exceeds target length 50: [', s.StoreRusBpo, N']')

                                  WHEN s.StoreRus_Clean IS NOT NULL
                                      AND LEN(s.StoreRus_Clean) > 50 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [StoreRus] exceeds target length 50: [', s.StoreRus, N']')

                                  WHEN s.MfpDivisionNew_Clean IS NOT NULL
                                      AND LEN(s.MfpDivisionNew_Clean) > 50 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [MfpDivisionNew] exceeds target length 50: [', s.MfpDivisionNew, N']')

                                  WHEN s.MfpDepartment_Clean IS NOT NULL
                                      AND LEN(s.MfpDepartment_Clean) > 50 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [MfpDepartment] exceeds target length 50: [', s.MfpDepartment, N']')

                                  WHEN s.MfpDivision_Clean IS NOT NULL
                                      AND LEN(s.MfpDivision_Clean) > 50 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [MfpDivision] exceeds target length 50: [', s.MfpDivision, N']')

                                  WHEN s.SkuSeasonBudget_Clean IS NOT NULL
                                      AND LEN(s.SkuSeasonBudget_Clean) > 20 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [SkuSeasonBudget] exceeds target length 20: [', s.SkuSeasonBudget, N']')

                                  WHEN s.TypeOfSales_Clean IS NOT NULL
                                      AND LEN(s.TypeOfSales_Clean) > 20 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [TypeOfSales] exceeds target length 20: [', s.TypeOfSales, N']')

                                  WHEN s.Season_Clean IS NOT NULL
                                      AND LEN(s.Season_Clean) > 20 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [Season] exceeds target length 20: [', s.Season, N']')

                                  WHEN s.Month_Clean IS NOT NULL
                                      AND LEN(s.Month_Clean) > 20 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [Month] exceeds target length 20: [', s.[Month], N']')

                                  WHEN s.Bundle_Clean IS NOT NULL
                                      AND LEN(s.Bundle_Clean) > 20 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [Bundle] exceeds target length 20: [', s.Bundle, N']')

                                  WHEN s.Seasonality_Clean IS NOT NULL
                                      AND LEN(s.Seasonality_Clean) > 20 THEN
                                      CONCAT(N'RawId=', s.Id, N'. Value in [Seasonality] exceeds target length 20: [', s.Seasonality, N']')

                                  ELSE NULL
                                  END
                      FROM Src s
                  )
         INSERT INTO dbo.Weekly_data_Load_error
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
        FROM dbo.Weekly_data_Load_error
        WHERE LoadSessionId = @LoadSessionId;

        /* 6. Если есть ошибки — ничего не переносим */
        IF @ErrorRows > 0
            BEGIN
                SET @Success = 0;
                SET @LoadedRows = 0;
                SET @Message = CONCAT(
                        N'Validation failed. Total raw rows: ', @TotalRows,
                        N'. Error rows: ', @ErrorRows,
                        N'. Nothing was loaded into dbo.Weekly_data.'
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
                          r.Id,
                          r.LoadSessionId,

                          Year21_Clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.Year21)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          Week21_Clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.Week21)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          YearCorr_Clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.YearCorr)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          WeekCorr_Clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.WeekCorr)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          Year_Clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.[Year])), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          Week_Clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.[Week])), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),

                          SalesChannelBpo_Clean = NULLIF(LTRIM(RTRIM(r.SalesChannelBpo)), N''),
                          StoreRusBpo_Clean = NULLIF(LTRIM(RTRIM(r.StoreRusBpo)), N''),
                          StoreRus_Clean = NULLIF(LTRIM(RTRIM(r.StoreRus)), N''),
                          MfpDivisionNew_Clean = NULLIF(LTRIM(RTRIM(r.MfpDivisionNew)), N''),
                          MfpDepartment_Clean = NULLIF(LTRIM(RTRIM(r.MfpDepartment)), N''),
                          SkuSeasonBudget_Clean = NULLIF(LTRIM(RTRIM(r.SkuSeasonBudget)), N''),
                          TypeOfSales_Clean = NULLIF(LTRIM(RTRIM(r.TypeOfSales)), N''),

                          TotalStockPcs_Clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.TotalStockPcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),

                          TotalStockDdp_Clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.TotalStockDdp)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),

                          SalesPcs_Clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.SalesPcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),

                          SalesRub_Clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.SalesRub)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),

                          Revenue_Clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.Revenue)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),

                          Gp_Clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.Gp)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),

                          DiscountTotalRub_Clean = NULLIF(
                                  REPLACE(
                                          REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.DiscountTotalRub)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                          N',', N'.'
                                      ),
                                  N''
                              ),

                          MfpDivision_Clean = NULLIF(LTRIM(RTRIM(r.MfpDivision)), N''),
                          Season_Clean = NULLIF(LTRIM(RTRIM(r.Season)), N''),
                          Month_Clean = NULLIF(LTRIM(RTRIM(r.[Month])), N''),
                          Bundle_Clean = NULLIF(LTRIM(RTRIM(r.Bundle)), N''),
                          Seasonality_Clean = NULLIF(LTRIM(RTRIM(r.Seasonality)), N'')
                      FROM dbo.Weekly_data_raw r
                      WHERE r.LoadSessionId = @LoadSessionId
                  )
         INSERT INTO dbo.Weekly_data
         (
             LoadSessionId,
             Year21,
             Week21,
             YearCorr,
             WeekCorr,
             Year,
             Week,
             SalesChannelBpo,
             StoreRusBpo,
             StoreRus,
             MfpDivisionNew,
             MfpDepartment,
             SkuSeasonBudget,
             TypeOfSales,
             TotalStockPcs,
             TotalStockDdp,
             SalesPcs,
             SalesRub,
             Revenue,
             Gp,
             DiscountTotalRub,
             MfpDivision,
             Season,
             Month,
             Bundle,
             Seasonality
         )
         SELECT
             s.LoadSessionId,
             TRY_CONVERT(SMALLINT, s.Year21_Clean),
             TRY_CONVERT(SMALLINT, s.Week21_Clean),
             TRY_CONVERT(SMALLINT, s.YearCorr_Clean),
             TRY_CONVERT(SMALLINT, s.WeekCorr_Clean),
             TRY_CONVERT(SMALLINT, s.Year_Clean),
             TRY_CONVERT(SMALLINT, s.Week_Clean),
             s.SalesChannelBpo_Clean,
             s.StoreRusBpo_Clean,
             s.StoreRus_Clean,
             s.MfpDivisionNew_Clean,
             s.MfpDepartment_Clean,
             s.SkuSeasonBudget_Clean,
             s.TypeOfSales_Clean,
             ISNULL(TRY_CONVERT(INT, s.TotalStockPcs_Clean), 0),
             ISNULL(TRY_CONVERT(DECIMAL(18,2), s.TotalStockDdp_Clean), 0),
             ISNULL(TRY_CONVERT(INT, s.SalesPcs_Clean), 0),
             ISNULL(TRY_CONVERT(DECIMAL(18,2), s.SalesRub_Clean), 0),
             ISNULL(TRY_CONVERT(DECIMAL(18,2), s.Revenue_Clean), 0),
             ISNULL(TRY_CONVERT(DECIMAL(18,2), s.Gp_Clean), 0),
             ISNULL(TRY_CONVERT(DECIMAL(18,2), s.DiscountTotalRub_Clean), 0),
             s.MfpDivision_Clean,
             s.Season_Clean,
             s.Month_Clean,
             s.Bundle_Clean,
             s.Seasonality_Clean
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
            INSERT INTO dbo.Weekly_data_Load_error
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
        FROM dbo.Weekly_data_Load_error
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