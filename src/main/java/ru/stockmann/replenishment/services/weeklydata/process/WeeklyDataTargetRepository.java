package ru.stockmann.replenishment.services.weeklydata.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class WeeklyDataTargetRepository {

    private static final Logger log = LoggerFactory.getLogger(WeeklyDataTargetRepository.class);

    public int publishFromStage(Connection connection, long loadSessionId) {
        deleteTargetRows(connection, loadSessionId);
        return insertFromStage(connection, loadSessionId);
    }

    private void deleteTargetRows(Connection connection, long loadSessionId) {
        String sql = """
                DELETE FROM dbo.Weekly_data
                WHERE LoadSessionId = ?
                """;

        long startedAt = System.nanoTime();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            int affectedRows = ps.executeUpdate();
            log.info("WeeklyData target delete completed. loadSessionId={}, affectedRows={}, elapsedMs={}",
                    loadSessionId, affectedRows, elapsedMs(startedAt));
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to delete Weekly_data rows. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    private int insertFromStage(Connection connection, long loadSessionId) {
        String sql = """
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
                FROM dbo.Weekly_data_stage
                WHERE LoadSessionId = ?
                """;

        long startedAt = System.nanoTime();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            int publishedRows = ps.executeUpdate();
            log.info("WeeklyData stage INSERT SELECT completed. loadSessionId={}, publishedRows={}, elapsedMs={}",
                    loadSessionId, publishedRows, elapsedMs(startedAt));
            return publishedRows;
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to publish Weekly_data_stage rows. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
