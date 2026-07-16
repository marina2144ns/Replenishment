package ru.stockmann.replenishment.services.weeklydata.process;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class WeeklyDataTargetRepository {

    private final DataSource dataSource;

    public WeeklyDataTargetRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void deleteByLoadSessionId(long loadSessionId) {
        try (Connection connection = dataSource.getConnection()) {
            deleteByLoadSessionId(connection, loadSessionId);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to delete Weekly_data rows. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    public void deleteByLoadSessionId(Connection connection, long loadSessionId) {
        String sql = """
                DELETE FROM dbo.Weekly_data
                WHERE LoadSessionId = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, loadSessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to delete Weekly_data rows. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    public void insertAll(List<WeeklyDataTargetRow> rows) {
        try (Connection connection = dataSource.getConnection()) {
            insertAll(connection, rows);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert Weekly_data rows", e);
        }
    }

    public void insertAll(Connection connection, List<WeeklyDataTargetRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

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
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            for (WeeklyDataTargetRow row : rows) {
                bindRow(ps, row);
                ps.addBatch();
            }

            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert Weekly_data rows", e);
        }
    }

    private void bindRow(PreparedStatement ps, WeeklyDataTargetRow row) throws SQLException {
        ps.setLong(1, row.loadSessionId());
        ps.setObject(2, row.year21());
        ps.setObject(3, row.week21());
        ps.setObject(4, row.yearCorr());
        ps.setObject(5, row.weekCorr());
        ps.setObject(6, row.year());
        ps.setObject(7, row.week());
        ps.setString(8, row.salesChannelBpo());
        ps.setString(9, row.storeRusBpo());
        ps.setString(10, row.storeRus());
        ps.setString(11, row.mfpDivisionNew());
        ps.setString(12, row.mfpDepartment());
        ps.setString(13, row.skuSeasonBudget());
        ps.setString(14, row.typeOfSales());
        ps.setBigDecimal(15, row.totalStockPcs());
        ps.setBigDecimal(16, row.totalStockDdp());
        ps.setBigDecimal(17, row.salesPcs());
        ps.setBigDecimal(18, row.salesRub());
        ps.setBigDecimal(19, row.revenue());
        ps.setBigDecimal(20, row.gp());
        ps.setBigDecimal(21, row.discountTotalRub());
        ps.setString(22, row.mfpDivision());
        ps.setString(23, row.season());
        ps.setString(24, row.month());
        ps.setString(25, row.bundle());
        ps.setString(26, row.seasonality());
    }
}
