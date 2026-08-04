package ru.stockmann.replenishment.services.salesbychannel.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SalesByChannelTargetRepository {

    private static final Logger log = LoggerFactory.getLogger(SalesByChannelTargetRepository.class);

    int publishFromStage(Connection connection, long loadSessionId) {
        deletePublicationScope(connection, loadSessionId);
        return insertFromStage(connection, loadSessionId);
    }

    private void deletePublicationScope(Connection connection, long loadSessionId) {
        String sql = """
                DELETE target
                FROM dbo.SalesByChannel AS target
                INNER JOIN
                (
                    SELECT DISTINCT [year], [month]
                    FROM dbo.SalesByChannel_stage
                    WHERE LoadSessionId = ?
                ) AS scope
                    ON scope.[year] = target.[year]
                   AND scope.[month] = target.[month]
                """;
        long startedAt = System.nanoTime();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            int affectedRows = ps.executeUpdate();
            log.info("SalesByChannel target scope delete completed. loadSessionId={}, affectedRows={}, "
                            + "elapsedMs={}",
                    loadSessionId, affectedRows, elapsedMs(startedAt));
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to delete SalesByChannel publication scope. loadSessionId=" + loadSessionId, e
            );
        }
    }

    private int insertFromStage(Connection connection, long loadSessionId) {
        String sql = """
                INSERT INTO dbo.SalesByChannel
                (
                    LoadSessionId,
                    seasonYear,
                    season6m,
                    yearMonth,
                    yearSeason,
                    [year],
                    [month],
                    salesChannelType,
                    storeRus,
                    typeOfSales,
                    mfpDivision,
                    mfpDepartment,
                    campaignSalesType,
                    seasonality,
                    skuBrandType,
                    salesQuantity,
                    salesCurr,
                    gm,
                    discountTtl,
                    turnoverCurr,
                    skuSeasonBudget,
                    storeRusBpo,
                    salesChannelBpo,
                    mfpSubDepartment,
                    skuTm,
                    mfpNode,
                    section,
                    merchandiseSubGroup,
                    skuPhase,
                    skuProductClass,
                    RawRowId
                )
                SELECT
                    LoadSessionId,
                    seasonYear,
                    season6m,
                    yearMonth,
                    yearSeason,
                    [year],
                    [month],
                    salesChannelType,
                    storeRus,
                    typeOfSales,
                    mfpDivision,
                    mfpDepartment,
                    campaignSalesType,
                    seasonality,
                    skuBrandType,
                    salesQuantity,
                    salesCurr,
                    gm,
                    discountTtl,
                    turnoverCurr,
                    skuSeasonBudget,
                    storeRusBpo,
                    salesChannelBpo,
                    mfpSubDepartment,
                    skuTm,
                    mfpNode,
                    section,
                    merchandiseSubGroup,
                    skuPhase,
                    skuProductClass,
                    RawRowId
                FROM dbo.SalesByChannel_stage
                WHERE LoadSessionId = ?
                """;
        long startedAt = System.nanoTime();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            int publishedRows = ps.executeUpdate();
            log.info("SalesByChannel stage INSERT SELECT completed. loadSessionId={}, publishedRows={}, "
                            + "elapsedMs={}",
                    loadSessionId, publishedRows, elapsedMs(startedAt));
            return publishedRows;
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to publish SalesByChannel_stage rows. loadSessionId=" + loadSessionId, e
            );
        }
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
