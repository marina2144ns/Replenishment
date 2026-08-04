package ru.stockmann.replenishment.services.weeklydata.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

public class WeeklyDataStageRepository {

    private static final Logger log = LoggerFactory.getLogger(WeeklyDataStageRepository.class);

    public WeeklyDataStageRepository(javax.sql.DataSource dataSource) {
    }

    public int deleteByLoadSessionId(Connection connection, long loadSessionId) {
        String sql = """
                DELETE FROM dbo.Weekly_data_stage
                WHERE LoadSessionId = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to delete Weekly_data_stage rows. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    public void insertBatch(Connection connection, long loadSessionId, List<WeeklyDataStageRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO dbo.Weekly_data_stage
                (
                    LoadSessionId,
                    ExcelRowNum,
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
                    Seasonality,
                    RawRowId
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        long startedAt = System.nanoTime();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (WeeklyDataStageRow row : rows) {
                bindRow(ps, row);
                ps.addBatch();
            }

            int[] updateCounts = ps.executeBatch();
            validateUpdateCounts(updateCounts, rows.size(), loadSessionId);
            ps.clearBatch();

            log.info("WeeklyData stage chunk inserted. loadSessionId={}, chunkSize={}, elapsedMs={}, "
                            + "updateCountsLength={}",
                    loadSessionId, rows.size(), elapsedMs(startedAt), updateCounts.length);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to insert Weekly_data_stage rows. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    private void bindRow(PreparedStatement ps, WeeklyDataStageRow row) throws SQLException {
        ps.setLong(1, row.loadSessionId());
        setNullableBigint(ps, 2, row.excelRowNum());
        setNullableSmallint(ps, 3, row.year21());
        setNullableSmallint(ps, 4, row.week21());
        setNullableSmallint(ps, 5, row.yearCorr());
        setNullableSmallint(ps, 6, row.weekCorr());
        ps.setShort(7, row.year());
        ps.setShort(8, row.week());
        ps.setString(9, row.salesChannelBpo());
        ps.setString(10, row.storeRusBpo());
        ps.setString(11, row.storeRus());
        ps.setString(12, row.mfpDivisionNew());
        ps.setString(13, row.mfpDepartment());
        ps.setString(14, row.skuSeasonBudget());
        ps.setString(15, row.typeOfSales());
        ps.setBigDecimal(16, row.totalStockPcs());
        ps.setBigDecimal(17, row.totalStockDdp());
        ps.setBigDecimal(18, row.salesPcs());
        ps.setBigDecimal(19, row.salesRub());
        ps.setBigDecimal(20, row.revenue());
        ps.setBigDecimal(21, row.gp());
        ps.setBigDecimal(22, row.discountTotalRub());
        ps.setString(23, row.mfpDivision());
        ps.setString(24, row.season());
        ps.setString(25, row.month());
        ps.setString(26, row.bundle());
        ps.setString(27, row.seasonality());
        setNullableBigint(ps, 28, row.rawRowId());
    }

    private void validateUpdateCounts(int[] updateCounts, int expected, long loadSessionId) {
        if (updateCounts == null || updateCounts.length != expected) {
            throw new IllegalStateException(
                    "Unexpected Weekly_data_stage update counts. loadSessionId=" + loadSessionId
                            + ", expected=" + expected
                            + ", actual=" + (updateCounts == null ? "null" : updateCounts.length)
            );
        }
        for (int updateCount : updateCounts) {
            if (updateCount == Statement.EXECUTE_FAILED) {
                throw new IllegalStateException(
                        "Weekly_data_stage batch contains failed statement. loadSessionId=" + loadSessionId
                );
            }
        }
    }

    private void setNullableBigint(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    private void setNullableSmallint(PreparedStatement ps, int index, Short value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.SMALLINT);
        } else {
            ps.setShort(index, value);
        }
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
