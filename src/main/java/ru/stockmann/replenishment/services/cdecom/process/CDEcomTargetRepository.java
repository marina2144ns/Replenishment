package ru.stockmann.replenishment.services.cdecom.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CDEcomTargetRepository {

    private static final Logger log = LoggerFactory.getLogger(CDEcomTargetRepository.class);

    public int publishFromStage(Connection connection, long loadSessionId) {
        deleteTargetRows(connection, loadSessionId);
        return insertFromStage(connection, loadSessionId);
    }

    private void deleteTargetRows(Connection connection, long loadSessionId) {
        String sql = """
                DELETE FROM dbo.CD_ecom
                WHERE LoadSessionId = ?
                """;

        long startedAt = System.nanoTime();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            int affectedRows = ps.executeUpdate();
            log.info("CDEcom target delete completed. loadSessionId={}, affectedRows={}, elapsedMs={}",
                    loadSessionId, affectedRows, elapsedMs(startedAt));
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to delete CD_ecom rows. loadSessionId=" + loadSessionId, e
            );
        }
    }

    private int insertFromStage(Connection connection, long loadSessionId) {
        String sql = """
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
                    skuCollection,
                    RawRowId
                )
                SELECT
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
                    skuCollection,
                    RawRowId
                FROM dbo.CD_ecom_stage
                WHERE LoadSessionId = ?
                """;

        long startedAt = System.nanoTime();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            int publishedRows = ps.executeUpdate();
            log.info("CDEcom stage INSERT SELECT completed. loadSessionId={}, publishedRows={}, elapsedMs={}",
                    loadSessionId, publishedRows, elapsedMs(startedAt));
            return publishedRows;
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to publish CD_ecom_stage rows. loadSessionId=" + loadSessionId, e
            );
        }
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
