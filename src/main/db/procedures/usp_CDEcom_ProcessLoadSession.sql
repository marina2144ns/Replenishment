USE ReplenishmentDWH;
GO

CREATE PROCEDURE dbo.usp_CDEcom_ProcessLoadSession
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
        IF NOT EXISTS
            (
                SELECT 1
                FROM dbo.CD_ecom_load_session
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

        DELETE FROM dbo.CD_ecom_load_error
        WHERE LoadSessionId = @LoadSessionId;

        DELETE FROM dbo.CD_ecom
        WHERE LoadSessionId = @LoadSessionId;

        SELECT
                @TotalRows = COUNT_BIG(*)
        FROM dbo.CD_ecom_raw
        WHERE LoadSessionId = @LoadSessionId;

        ;WITH Src AS
                  (
                      SELECT
                          r.Id,
                          r.LoadSessionId,

                          r.name,
                          r.[year],
                          r.season,
                          r.[day],
                          r.[data],
                          r.salesChannelBpo,
                          r.storeRus,
                          r.mfpDivision,
                          r.mfpDepartment,
                          r.mfpSubDepartment,
                          r.skuBrandType,
                          r.skuTm,
                          r.mfpNode,
                          r.section,
                          r.merchandiseSubGroup,
                          r.campaignSalesType,
                          r.skuStyleColor,
                          r.skuPhase,
                          r.orderPcs,
                          r.orderRub,
                          r.foundPcs,
                          r.foundRub,
                          r.salesPcs,
                          r.salesRub,
                          r.revenue,
                          r.gp,
                          r.cogs,
                          r.salesDiscount,
                          r.planRub,
                          r.stockStoresPcs,
                          r.stockStoresDdp,
                          r.cdDrivers,
                          r.skuSupplierModel,
                          r.skuComposition,
                          r.skuColorRussian,
                          r.skuName,
                          r.skuCommentBuyer,
                          r.skuCollection,

                          name_clean = NULLIF(LTRIM(RTRIM(r.name)), N''),

                          year_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.[year])), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          season_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.season)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          day_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.[day])), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          data_clean = NULLIF(LTRIM(RTRIM(r.[data])), N''),

                          salesChannelBpo_clean = NULLIF(LTRIM(RTRIM(r.salesChannelBpo)), N''),
                          storeRus_clean = NULLIF(LTRIM(RTRIM(r.storeRus)), N''),
                          mfpDivision_clean = NULLIF(LTRIM(RTRIM(r.mfpDivision)), N''),
                          mfpDepartment_clean = NULLIF(LTRIM(RTRIM(r.mfpDepartment)), N''),
                          mfpSubDepartment_clean = NULLIF(LTRIM(RTRIM(r.mfpSubDepartment)), N''),
                          skuBrandType_clean = NULLIF(LTRIM(RTRIM(r.skuBrandType)), N''),
                          skuTm_clean = NULLIF(LTRIM(RTRIM(r.skuTm)), N''),
                          mfpNode_clean = NULLIF(LTRIM(RTRIM(r.mfpNode)), N''),
                          section_clean = NULLIF(LTRIM(RTRIM(r.section)), N''),
                          merchandiseSubGroup_clean = NULLIF(LTRIM(RTRIM(r.merchandiseSubGroup)), N''),
                          campaignSalesType_clean = NULLIF(LTRIM(RTRIM(r.campaignSalesType)), N''),
                          skuPhase_clean = NULLIF(LTRIM(RTRIM(r.skuPhase)), N''),
                          cdDrivers_clean = NULLIF(LTRIM(RTRIM(r.cdDrivers)), N''),
                          skuSupplierModel_clean = NULLIF(LTRIM(RTRIM(r.skuSupplierModel)), N''),
                          skuComposition_clean = NULLIF(LTRIM(RTRIM(r.skuComposition)), N''),
                          skuColorRussian_clean = NULLIF(LTRIM(RTRIM(r.skuColorRussian)), N''),
                          skuName_clean = NULLIF(LTRIM(RTRIM(r.skuName)), N''),
                          skuCommentBuyer_clean = NULLIF(LTRIM(RTRIM(r.skuCommentBuyer)), N''),
                          skuCollection_clean = NULLIF(LTRIM(RTRIM(r.skuCollection)), N''),

                          skuStyleColor_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.skuStyleColor)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),

                          orderPcs_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.orderPcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),
                          orderRub_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.orderRub)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),
                          foundPcs_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.foundPcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),
                          foundRub_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.foundRub)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),
                          salesPcs_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.salesPcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),
                          salesRub_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.salesRub)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),
                          revenue_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.revenue)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),
                          gp_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.gp)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),
                          cogs_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.cogs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),
                          salesDiscount_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.salesDiscount)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),

                          planRub_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.planRub)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          stockStoresPcs_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.stockStoresPcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          stockStoresDdp_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.stockStoresDdp)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              )
                      FROM dbo.CD_ecom_raw r
                      WHERE r.LoadSessionId = @LoadSessionId
                  ),
              Parsed AS
                  (
                      SELECT
                          s.*,

                          data_parsed = COALESCE(
                                  TRY_CONVERT(DATE, s.data_clean, 104),
                                  TRY_CONVERT(DATE, s.data_clean, 103),
                                  TRY_CONVERT(DATE, s.data_clean, 23),
                                  TRY_CONVERT(DATE, s.data_clean, 1),
                                  TRY_CONVERT(DATE, s.data_clean, 101)
                              ),

                          skuStyleColor_num = TRY_CONVERT(FLOAT, s.skuStyleColor_clean),

                          orderPcs_num = TRY_CONVERT(FLOAT, s.orderPcs_clean),
                          orderRub_num = TRY_CONVERT(FLOAT, s.orderRub_clean),
                          foundPcs_num = TRY_CONVERT(FLOAT, s.foundPcs_clean),
                          foundRub_num = TRY_CONVERT(FLOAT, s.foundRub_clean),
                          salesPcs_num = TRY_CONVERT(FLOAT, s.salesPcs_clean),
                          salesRub_num = TRY_CONVERT(FLOAT, s.salesRub_clean),
                          revenue_num = TRY_CONVERT(FLOAT, s.revenue_clean),
                          gp_num = TRY_CONVERT(FLOAT, s.gp_clean),
                          cogs_num = TRY_CONVERT(FLOAT, s.cogs_clean),
                          salesDiscount_num = TRY_CONVERT(FLOAT, s.salesDiscount_clean)
                      FROM Src s
                  ),
              Validation AS
                  (
                      SELECT
                          p.Id,
                          ErrorMessage =
                              CASE
                                  WHEN p.year_clean IS NOT NULL
                                      AND TRY_CONVERT(INT, p.year_clean) IS NULL THEN
                                      CONCAT(N'RawId=', p.Id, N'. Invalid INT value in [year]: [', p.[year], N']')

                                  WHEN p.season_clean IS NOT NULL
                                      AND TRY_CONVERT(INT, p.season_clean) IS NULL THEN
                                      CONCAT(N'RawId=', p.Id, N'. Invalid INT value in [season]: [', p.season, N']')

                                  WHEN p.day_clean IS NOT NULL
                                      AND TRY_CONVERT(INT, p.day_clean) IS NULL THEN
                                      CONCAT(N'RawId=', p.Id, N'. Invalid INT value in [day]: [', p.[day], N']')

                                  WHEN p.data_clean IS NOT NULL
                                      AND p.data_parsed IS NULL THEN
                                      CONCAT(N'RawId=', p.Id, N'. Invalid DATE value in [data]: [', p.[data], N']')

                                  WHEN p.skuStyleColor_clean IS NOT NULL
                                      AND p.skuStyleColor_num IS NULL THEN
                                      CONCAT(N'RawId=', p.Id, N'. Invalid BIGINT value in [skuStyleColor]: [', p.skuStyleColor, N']')

                                  WHEN p.orderPcs_clean IS NOT NULL
                                      AND p.orderPcs_num IS NULL THEN
                                      CONCAT(N'RawId=', p.Id, N'. Invalid DECIMAL(18,2) value in [orderPcs]: [', p.orderPcs, N']')

                                  WHEN p.orderRub_clean IS NOT NULL
                                      AND p.orderRub_num IS NULL THEN
                                      CONCAT(N'RawId=', p.Id, N'. Invalid DECIMAL(18,2) value in [orderRub]: [', p.orderRub, N']')

                                  WHEN p.foundPcs_clean IS NOT NULL
                                      AND p.foundPcs_num IS NULL THEN
                                      CONCAT(N'RawId=', p.Id, N'. Invalid DECIMAL(18,2) value in [foundPcs]: [', p.foundPcs, N']')

                                  WHEN p.foundRub_clean IS NOT NULL
                                      AND p.foundRub_num IS NULL THEN
                                      CONCAT(N'RawId=', p.Id, N'. Invalid DECIMAL(18,2) value in [foundRub]: [', p.foundRub, N']')

                                  WHEN p.salesPcs_clean IS NOT NULL
                                      AND p.salesPcs_num IS NULL THEN
                                      CONCAT(N'RawId=', p.Id, N'. Invalid DECIMAL(18,2) value in [salesPcs]: [', p.salesPcs, N']')

                                  WHEN p.salesRub_clean IS NOT NULL
                                      AND p.salesRub_num IS NULL THEN
                                      CONCAT(N'RawId=', p.Id, N'. Invalid DECIMAL(18,2) value in [salesRub]: [', p.salesRub, N']')

                                  WHEN p.revenue_clean IS NOT NULL
                                      AND p.revenue_num IS NULL THEN
                                      CONCAT(N'RawId=', p.Id, N'. Invalid DECIMAL(18,2) value in [revenue]: [', p.revenue, N']')

                                  WHEN p.gp_clean IS NOT NULL
                                      AND p.gp_num IS NULL THEN
                                      CONCAT(N'RawId=', p.Id, N'. Invalid DECIMAL(18,2) value in [gp]: [', p.gp, N']')

                                  WHEN p.cogs_clean IS NOT NULL
                                      AND p.cogs_num IS NULL THEN
                                      CONCAT(N'RawId=', p.Id, N'. Invalid DECIMAL(18,2) value in [cogs]: [', p.cogs, N']')

                                  WHEN p.salesDiscount_clean IS NOT NULL
                                      AND p.salesDiscount_num IS NULL THEN
                                      CONCAT(N'RawId=', p.Id, N'. Invalid DECIMAL(18,2) value in [salesDiscount]: [', p.salesDiscount, N']')

                                  WHEN p.planRub_clean IS NOT NULL
                                      AND TRY_CONVERT(BIGINT, p.planRub_clean) IS NULL THEN
                                      CONCAT(N'RawId=', p.Id, N'. Invalid BIGINT value in [planRub]: [', p.planRub, N']')

                                  WHEN p.stockStoresPcs_clean IS NOT NULL
                                      AND TRY_CONVERT(BIGINT, p.stockStoresPcs_clean) IS NULL THEN
                                      CONCAT(N'RawId=', p.Id, N'. Invalid BIGINT value in [stockStoresPcs]: [', p.stockStoresPcs, N']')

                                  WHEN p.stockStoresDdp_clean IS NOT NULL
                                      AND TRY_CONVERT(BIGINT, p.stockStoresDdp_clean) IS NULL THEN
                                      CONCAT(N'RawId=', p.Id, N'. Invalid BIGINT value in [stockStoresDdp]: [', p.stockStoresDdp, N']')

                                  WHEN p.name_clean IS NOT NULL
                                      AND LEN(p.name_clean) > 255 THEN
                                      CONCAT(N'RawId=', p.Id, N'. Value in [name] exceeds target length 255: [', p.name, N']')

                                  WHEN p.salesChannelBpo_clean IS NOT NULL
                                      AND LEN(p.salesChannelBpo_clean) > 255 THEN
                                      CONCAT(N'RawId=', p.Id, N'. Value in [salesChannelBpo] exceeds target length 255: [', p.salesChannelBpo, N']')

                                  WHEN p.storeRus_clean IS NOT NULL
                                      AND LEN(p.storeRus_clean) > 255 THEN
                                      CONCAT(N'RawId=', p.Id, N'. Value in [storeRus] exceeds target length 255: [', p.storeRus, N']')

                                  WHEN p.mfpDivision_clean IS NOT NULL
                                      AND LEN(p.mfpDivision_clean) > 255 THEN
                                      CONCAT(N'RawId=', p.Id, N'. Value in [mfpDivision] exceeds target length 255: [', p.mfpDivision, N']')

                                  WHEN p.mfpDepartment_clean IS NOT NULL
                                      AND LEN(p.mfpDepartment_clean) > 255 THEN
                                      CONCAT(N'RawId=', p.Id, N'. Value in [mfpDepartment] exceeds target length 255: [', p.mfpDepartment, N']')

                                  WHEN p.mfpSubDepartment_clean IS NOT NULL
                                      AND LEN(p.mfpSubDepartment_clean) > 255 THEN
                                      CONCAT(N'RawId=', p.Id, N'. Value in [mfpSubDepartment] exceeds target length 255: [', p.mfpSubDepartment, N']')

                                  WHEN p.skuBrandType_clean IS NOT NULL
                                      AND LEN(p.skuBrandType_clean) > 255 THEN
                                      CONCAT(N'RawId=', p.Id, N'. Value in [skuBrandType] exceeds target length 255: [', p.skuBrandType, N']')

                                  WHEN p.skuTm_clean IS NOT NULL
                                      AND LEN(p.skuTm_clean) > 255 THEN
                                      CONCAT(N'RawId=', p.Id, N'. Value in [skuTm] exceeds target length 255: [', p.skuTm, N']')

                                  WHEN p.mfpNode_clean IS NOT NULL
                                      AND LEN(p.mfpNode_clean) > 255 THEN
                                      CONCAT(N'RawId=', p.Id, N'. Value in [mfpNode] exceeds target length 255: [', p.mfpNode, N']')

                                  WHEN p.section_clean IS NOT NULL
                                      AND LEN(p.section_clean) > 255 THEN
                                      CONCAT(N'RawId=', p.Id, N'. Value in [section] exceeds target length 255: [', p.section, N']')

                                  WHEN p.merchandiseSubGroup_clean IS NOT NULL
                                      AND LEN(p.merchandiseSubGroup_clean) > 255 THEN
                                      CONCAT(N'RawId=', p.Id, N'. Value in [merchandiseSubGroup] exceeds target length 255: [', p.merchandiseSubGroup, N']')

                                  WHEN p.campaignSalesType_clean IS NOT NULL
                                      AND LEN(p.campaignSalesType_clean) > 255 THEN
                                      CONCAT(N'RawId=', p.Id, N'. Value in [campaignSalesType] exceeds target length 255: [', p.campaignSalesType, N']')

                                  WHEN p.skuPhase_clean IS NOT NULL
                                      AND LEN(p.skuPhase_clean) > 255 THEN
                                      CONCAT(N'RawId=', p.Id, N'. Value in [skuPhase] exceeds target length 255: [', p.skuPhase, N']')

                                  WHEN p.cdDrivers_clean IS NOT NULL
                                      AND LEN(p.cdDrivers_clean) > 255 THEN
                                      CONCAT(N'RawId=', p.Id, N'. Value in [cdDrivers] exceeds target length 255: [', p.cdDrivers, N']')

                                  WHEN p.skuSupplierModel_clean IS NOT NULL
                                      AND LEN(p.skuSupplierModel_clean) > 255 THEN
                                      CONCAT(N'RawId=', p.Id, N'. Value in [skuSupplierModel] exceeds target length 255: [', p.skuSupplierModel, N']')

                                  WHEN p.skuComposition_clean IS NOT NULL
                                      AND LEN(p.skuComposition_clean) > 255 THEN
                                      CONCAT(N'RawId=', p.Id, N'. Value in [skuComposition] exceeds target length 255: [', p.skuComposition, N']')

                                  WHEN p.skuColorRussian_clean IS NOT NULL
                                      AND LEN(p.skuColorRussian_clean) > 255 THEN
                                      CONCAT(N'RawId=', p.Id, N'. Value in [skuColorRussian] exceeds target length 255: [', p.skuColorRussian, N']')

                                  WHEN p.skuName_clean IS NOT NULL
                                      AND LEN(p.skuName_clean) > 255 THEN
                                      CONCAT(N'RawId=', p.Id, N'. Value in [skuName] exceeds target length 255: [', p.skuName, N']')

                                  WHEN p.skuCommentBuyer_clean IS NOT NULL
                                      AND LEN(p.skuCommentBuyer_clean) > 255 THEN
                                      CONCAT(N'RawId=', p.Id, N'. Value in [skuCommentBuyer] exceeds target length 255: [', p.skuCommentBuyer, N']')

                                  WHEN p.skuCollection_clean IS NOT NULL
                                      AND LEN(p.skuCollection_clean) > 255 THEN
                                      CONCAT(N'RawId=', p.Id, N'. Value in [skuCollection] exceeds target length 255: [', p.skuCollection, N']')

                                  ELSE NULL
                                  END
                      FROM Parsed p
                  )
         INSERT INTO dbo.CD_ecom_load_error
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

        SELECT
                @ErrorRows = COUNT_BIG(*)
        FROM dbo.CD_ecom_load_error
        WHERE LoadSessionId = @LoadSessionId;

        IF @ErrorRows > 0
            BEGIN
                SET @Success = 0;
                SET @LoadedRows = 0;
                SET @Message = CONCAT(
                        N'Validation failed. Total raw rows: ', @TotalRows,
                        N'. Error rows: ', @ErrorRows,
                        N'. Nothing was loaded into dbo.CD_ecom.'
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

        BEGIN TRANSACTION;

        ;WITH Src AS
                  (
                      SELECT
                          r.LoadSessionId,

                          name_clean = NULLIF(LTRIM(RTRIM(r.name)), N''),

                          year_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.[year])), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          season_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.season)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          day_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.[day])), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          data_clean = NULLIF(LTRIM(RTRIM(r.[data])), N''),

                          salesChannelBpo_clean = NULLIF(LTRIM(RTRIM(r.salesChannelBpo)), N''),
                          storeRus_clean = NULLIF(LTRIM(RTRIM(r.storeRus)), N''),
                          mfpDivision_clean = NULLIF(LTRIM(RTRIM(r.mfpDivision)), N''),
                          mfpDepartment_clean = NULLIF(LTRIM(RTRIM(r.mfpDepartment)), N''),
                          mfpSubDepartment_clean = NULLIF(LTRIM(RTRIM(r.mfpSubDepartment)), N''),
                          skuBrandType_clean = NULLIF(LTRIM(RTRIM(r.skuBrandType)), N''),
                          skuTm_clean = NULLIF(LTRIM(RTRIM(r.skuTm)), N''),
                          mfpNode_clean = NULLIF(LTRIM(RTRIM(r.mfpNode)), N''),
                          section_clean = NULLIF(LTRIM(RTRIM(r.section)), N''),
                          merchandiseSubGroup_clean = NULLIF(LTRIM(RTRIM(r.merchandiseSubGroup)), N''),
                          campaignSalesType_clean = NULLIF(LTRIM(RTRIM(r.campaignSalesType)), N''),
                          skuPhase_clean = NULLIF(LTRIM(RTRIM(r.skuPhase)), N''),
                          cdDrivers_clean = NULLIF(LTRIM(RTRIM(r.cdDrivers)), N''),
                          skuSupplierModel_clean = NULLIF(LTRIM(RTRIM(r.skuSupplierModel)), N''),
                          skuComposition_clean = NULLIF(LTRIM(RTRIM(r.skuComposition)), N''),
                          skuColorRussian_clean = NULLIF(LTRIM(RTRIM(r.skuColorRussian)), N''),
                          skuName_clean = NULLIF(LTRIM(RTRIM(r.skuName)), N''),
                          skuCommentBuyer_clean = NULLIF(LTRIM(RTRIM(r.skuCommentBuyer)), N''),
                          skuCollection_clean = NULLIF(LTRIM(RTRIM(r.skuCollection)), N''),

                          skuStyleColor_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.skuStyleColor)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),

                          orderPcs_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.orderPcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),
                          orderRub_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.orderRub)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),
                          foundPcs_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.foundPcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),
                          foundRub_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.foundRub)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),
                          salesPcs_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.salesPcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),
                          salesRub_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.salesRub)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),
                          revenue_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.revenue)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),
                          gp_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.gp)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),
                          cogs_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.cogs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),
                          salesDiscount_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.salesDiscount)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''), N',', N'.'),
                                  N''
                              ),

                          planRub_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.planRub)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          stockStoresPcs_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.stockStoresPcs)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              ),
                          stockStoresDdp_clean = NULLIF(
                                  REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(r.stockStoresDdp)), NCHAR(160), N''), NCHAR(8239), N''), N' ', N''),
                                  N''
                              )
                      FROM dbo.CD_ecom_raw r
                      WHERE r.LoadSessionId = @LoadSessionId
                  ),
              Parsed AS
                  (
                      SELECT
                          s.LoadSessionId,
                          s.name_clean,
                          TRY_CONVERT(INT, s.year_clean) AS year_val,
                          TRY_CONVERT(INT, s.season_clean) AS season_val,
                          TRY_CONVERT(INT, s.day_clean) AS day_val,
                          COALESCE(
                                  TRY_CONVERT(DATE, s.data_clean, 104),
                                  TRY_CONVERT(DATE, s.data_clean, 103),
                                  TRY_CONVERT(DATE, s.data_clean, 23),
                                  TRY_CONVERT(DATE, s.data_clean, 1),
                                  TRY_CONVERT(DATE, s.data_clean, 101)
                              ) AS data_val,
                          s.salesChannelBpo_clean,
                          s.storeRus_clean,
                          s.mfpDivision_clean,
                          s.mfpDepartment_clean,
                          s.mfpSubDepartment_clean,
                          s.skuBrandType_clean,
                          s.skuTm_clean,
                          s.mfpNode_clean,
                          s.section_clean,
                          s.merchandiseSubGroup_clean,
                          s.campaignSalesType_clean,
                          TRY_CONVERT(BIGINT, ROUND(TRY_CONVERT(FLOAT, s.skuStyleColor_clean), 0)) AS skuStyleColor_val,
                          s.skuPhase_clean,

                          CAST(ROUND(TRY_CONVERT(FLOAT, s.orderPcs_clean), 2) AS DECIMAL(18,2)) AS orderPcs_val,
                          CAST(ROUND(TRY_CONVERT(FLOAT, s.orderRub_clean), 2) AS DECIMAL(18,2)) AS orderRub_val,
                          CAST(ROUND(TRY_CONVERT(FLOAT, s.foundPcs_clean), 2) AS DECIMAL(18,2)) AS foundPcs_val,
                          CAST(ROUND(TRY_CONVERT(FLOAT, s.foundRub_clean), 2) AS DECIMAL(18,2)) AS foundRub_val,
                          CAST(ROUND(TRY_CONVERT(FLOAT, s.salesPcs_clean), 2) AS DECIMAL(18,2)) AS salesPcs_val,
                          CAST(ROUND(TRY_CONVERT(FLOAT, s.salesRub_clean), 2) AS DECIMAL(18,2)) AS salesRub_val,
                          CAST(ROUND(TRY_CONVERT(FLOAT, s.revenue_clean), 2) AS DECIMAL(18,2)) AS revenue_val,
                          CAST(ROUND(TRY_CONVERT(FLOAT, s.gp_clean), 2) AS DECIMAL(18,2)) AS gp_val,
                          CAST(ROUND(TRY_CONVERT(FLOAT, s.cogs_clean), 2) AS DECIMAL(18,2)) AS cogs_val,
                          CAST(ROUND(TRY_CONVERT(FLOAT, s.salesDiscount_clean), 2) AS DECIMAL(18,2)) AS salesDiscount_val,

                          TRY_CONVERT(BIGINT, s.planRub_clean) AS planRub_val,
                          TRY_CONVERT(BIGINT, s.stockStoresPcs_clean) AS stockStoresPcs_val,
                          TRY_CONVERT(BIGINT, s.stockStoresDdp_clean) AS stockStoresDdp_val,

                          s.cdDrivers_clean,
                          s.skuSupplierModel_clean,
                          s.skuComposition_clean,
                          s.skuColorRussian_clean,
                          s.skuName_clean,
                          s.skuCommentBuyer_clean,
                          s.skuCollection_clean
                      FROM Src s
                  )
         INSERT INTO dbo.CD_ecom
         (
             LoadSessionId,
             name,
             [year],
             season,
             [day],
             [data],
             salesChannelBpo,
             storeRus,
             mfpDivision,
             mfpDepartment,
             mfpSubDepartment,
             skuBrandType,
             skuTm,
             mfpNode,
             section,
             merchandiseSubGroup,
             campaignSalesType,
             skuStyleColor,
             skuPhase,
             orderPcs,
             orderRub,
             foundPcs,
             foundRub,
             salesPcs,
             salesRub,
             revenue,
             gp,
             cogs,
             salesDiscount,
             planRub,
             stockStoresPcs,
             stockStoresDdp,
             cdDrivers,
             skuSupplierModel,
             skuComposition,
             skuColorRussian,
             skuName,
             skuCommentBuyer,
             skuCollection
         )
         SELECT
             p.LoadSessionId,
             p.name_clean,
             p.year_val,
             p.season_val,
             p.day_val,
             p.data_val,
             p.salesChannelBpo_clean,
             p.storeRus_clean,
             p.mfpDivision_clean,
             p.mfpDepartment_clean,
             p.mfpSubDepartment_clean,
             p.skuBrandType_clean,
             p.skuTm_clean,
             p.mfpNode_clean,
             p.section_clean,
             p.merchandiseSubGroup_clean,
             p.campaignSalesType_clean,
             p.skuStyleColor_val,
             p.skuPhase_clean,
             p.orderPcs_val,
             p.orderRub_val,
             p.foundPcs_val,
             p.foundRub_val,
             p.salesPcs_val,
             p.salesRub_val,
             p.revenue_val,
             p.gp_val,
             p.cogs_val,
             p.salesDiscount_val,
             p.planRub_val,
             p.stockStoresPcs_val,
             p.stockStoresDdp_val,
             p.cdDrivers_clean,
             p.skuSupplierModel_clean,
             p.skuComposition_clean,
             p.skuColorRussian_clean,
             p.skuName_clean,
             p.skuCommentBuyer_clean,
             p.skuCollection_clean
         FROM Parsed p;

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
            INSERT INTO dbo.CD_ecom_load_error
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
        END CATCH;

        SELECT
                @ErrorRows = COUNT_BIG(*)
        FROM dbo.CD_ecom_load_error
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