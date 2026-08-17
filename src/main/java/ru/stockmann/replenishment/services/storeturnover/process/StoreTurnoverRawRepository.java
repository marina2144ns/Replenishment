package ru.stockmann.replenishment.services.storeturnover.process;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StoreTurnoverRawRepository {
    public static final long INITIAL_LAST_RAW_ID = 0L;
    private final DataSource dataSource;
    private final int chunkSize;
    public StoreTurnoverRawRepository(DataSource dataSource, int chunkSize) { this.dataSource = dataSource; this.chunkSize = chunkSize; }

    public List<StoreTurnoverRawRow> findChunk(long session, long lastId) {
        try (Connection c = dataSource.getConnection()) { return findChunk(c, session, lastId, chunkSize); }
        catch (SQLException e) { throw new RuntimeException("Failed to read StoreTurnover_raw", e); }
    }

    public List<StoreTurnoverRawRow> findChunk(Connection c, long session, long lastId, int limit) {
        String sql = """
                SELECT TOP (?) Id, LoadSessionId, ExcelRowNum, sku, period, storeRus,
                    remainingSum, remainingDays, salesQuantity, sales, asp, revenue, gp, discountTotal
                FROM dbo.StoreTurnover_raw
                WHERE LoadSessionId = ? AND Id > ? ORDER BY Id
                """;
        List<StoreTurnoverRawRow> rows = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit); ps.setLong(2, session); ps.setLong(3, lastId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(new StoreTurnoverRawRow(
                        rs.getLong("Id"), rs.getLong("LoadSessionId"), nullableLong(rs, "ExcelRowNum"),
                        rs.getString("sku"), rs.getString("period"), rs.getString("storeRus"),
                        rs.getString("remainingSum"), rs.getString("remainingDays"),
                        rs.getString("salesQuantity"), rs.getString("sales"), rs.getString("asp"),
                        rs.getString("revenue"), rs.getString("gp"), rs.getString("discountTotal")));
            }
            return rows;
        } catch (SQLException e) { throw new RuntimeException("Failed to read StoreTurnover_raw", e); }
    }
    private Long nullableLong(ResultSet rs, String name) throws SQLException { long v=rs.getLong(name); return rs.wasNull()?null:v; }
}
