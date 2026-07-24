package ru.stockmann.replenishment.services.weeklydata.process;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WeeklyDataRawRepository {

    public static final int DEFAULT_CHUNK_SIZE = 1_000;
    public static final long INITIAL_LAST_RAW_ID = 0L;

    private final DataSource dataSource;

    public WeeklyDataRawRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<WeeklyDataRawRow> findChunk(long loadSessionId, long lastRawId) {
        try (Connection connection = dataSource.getConnection()) {
            return findChunk(connection, loadSessionId, lastRawId, DEFAULT_CHUNK_SIZE);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to read Weekly_data_raw rows. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    public List<WeeklyDataRawRow> findChunk(
            Connection connection,
            long loadSessionId,
            long lastRawId,
            int chunkSize
    ) {
        String sql = """
                SELECT TOP (?)
                    Id AS rawId,
                    LoadSessionId,
                    ExcelRowNum,
                    Year21,
                    Week21,
                    YearCorr,
                    WeekCorr,
                    [Year],
                    [Week],
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
                    [Month],
                    Bundle,
                    Seasonality
                FROM dbo.Weekly_data_raw
                WHERE LoadSessionId = ?
                  AND Id > ?
                ORDER BY Id
                """;

        List<WeeklyDataRawRow> rows = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, chunkSize);
            ps.setLong(2, loadSessionId);
            ps.setLong(3, lastRawId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapRow(rs));
                }
            }

            return rows;
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to read Weekly_data_raw rows. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    private WeeklyDataRawRow mapRow(ResultSet rs) throws SQLException {
        return new WeeklyDataRawRow(
                rs.getLong("LoadSessionId"),
                rs.getLong("rawId"),
                getNullableLong(rs, "ExcelRowNum"),
                rs.getString("Year21"),
                rs.getString("Week21"),
                rs.getString("YearCorr"),
                rs.getString("WeekCorr"),
                rs.getString("Year"),
                rs.getString("Week"),
                rs.getString("SalesChannelBpo"),
                rs.getString("StoreRusBpo"),
                rs.getString("StoreRus"),
                rs.getString("MfpDivisionNew"),
                rs.getString("MfpDepartment"),
                rs.getString("SkuSeasonBudget"),
                rs.getString("TypeOfSales"),
                rs.getString("TotalStockPcs"),
                rs.getString("TotalStockDdp"),
                rs.getString("SalesPcs"),
                rs.getString("SalesRub"),
                rs.getString("Revenue"),
                rs.getString("Gp"),
                rs.getString("DiscountTotalRub"),
                rs.getString("MfpDivision"),
                rs.getString("Season"),
                rs.getString("Month"),
                rs.getString("Bundle"),
                rs.getString("Seasonality")
        );
    }

    private Long getNullableLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }
}
