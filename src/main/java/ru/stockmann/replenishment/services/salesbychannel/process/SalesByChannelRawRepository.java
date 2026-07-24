package ru.stockmann.replenishment.services.salesbychannel.process;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SalesByChannelRawRepository {

    public static final long INITIAL_LAST_RAW_ID = 0L;
    private final DataSource dataSource;
    private final int chunkSize;

    public SalesByChannelRawRepository(DataSource dataSource, int chunkSize) {
        this.dataSource = dataSource;
        this.chunkSize = chunkSize;
    }

    public List<SalesByChannelRawRow> findChunk(long loadSessionId, long lastRawId) {
        try (Connection connection = dataSource.getConnection()) {
            return findChunk(connection, loadSessionId, lastRawId, chunkSize);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to read SalesByChannel_raw chunk. loadSessionId=" + loadSessionId, e
            );
        }
    }

    public List<SalesByChannelRawRow> findChunk(
            Connection connection,
            long loadSessionId,
            long lastRawId,
            int limit
    ) {
        String sql = """
                SELECT TOP (?)
                    Id, LoadSessionId, ExcelRowNum,
                    seasonYear, season6m, yearMonth, yearSeason, [year], [month],
                    salesChannelType, storeRus, typeOfSales, mfpDivision, mfpDepartment,
                    campaignSalesType, seasonality, skuBrandType, salesQuantity, salesCurr,
                    gm, discountTtl, turnoverCurr, skuSeasonBudget, storeRusBpo,
                    salesChannelBpo, mfpSubDepartment, skuTm, mfpNode, section,
                    merchandiseSubGroup, skuPhase, skuProductClass
                FROM dbo.SalesByChannel_raw
                WHERE LoadSessionId = ?
                  AND Id > ?
                ORDER BY Id
                """;
        List<SalesByChannelRawRow> rows = new ArrayList<>(limit);
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
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
                    "Failed to read SalesByChannel_raw chunk. loadSessionId=" + loadSessionId, e
            );
        }
    }

    private SalesByChannelRawRow mapRow(ResultSet rs) throws SQLException {
        return new SalesByChannelRawRow(
                rs.getLong("Id"), rs.getLong("LoadSessionId"), nullableLong(rs, "ExcelRowNum"),
                rs.getString("seasonYear"), rs.getString("season6m"), rs.getString("yearMonth"),
                rs.getString("yearSeason"), rs.getString("year"), rs.getString("month"),
                rs.getString("salesChannelType"), rs.getString("storeRus"), rs.getString("typeOfSales"),
                rs.getString("mfpDivision"), rs.getString("mfpDepartment"),
                rs.getString("campaignSalesType"), rs.getString("seasonality"),
                rs.getString("skuBrandType"), rs.getString("salesQuantity"), rs.getString("salesCurr"),
                rs.getString("gm"), rs.getString("discountTtl"), rs.getString("turnoverCurr"),
                rs.getString("skuSeasonBudget"), rs.getString("storeRusBpo"),
                rs.getString("salesChannelBpo"), rs.getString("mfpSubDepartment"),
                rs.getString("skuTm"), rs.getString("mfpNode"), rs.getString("section"),
                rs.getString("merchandiseSubGroup"), rs.getString("skuPhase"),
                rs.getString("skuProductClass")
        );
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
