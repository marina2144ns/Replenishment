package ru.stockmann.replenishment.services.salesbychannel.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

public class SalesByChannelStageRepository {

    private static final Logger log = LoggerFactory.getLogger(SalesByChannelStageRepository.class);

    public int deleteByLoadSessionId(Connection connection, long loadSessionId) {
        String sql = "DELETE FROM dbo.SalesByChannel_stage WHERE LoadSessionId = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to delete SalesByChannel_stage rows. loadSessionId=" + loadSessionId, e
            );
        }
    }

    public void insertBatch(
            Connection connection,
            long loadSessionId,
            List<SalesByChannelStageRow> rows
    ) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO dbo.SalesByChannel_stage
                (
                    LoadSessionId, ExcelRowNum,
                    seasonYear, season6m, yearMonth, yearSeason, [year], [month],
                    salesChannelType, storeRus, typeOfSales, mfpDivision, mfpDepartment,
                    campaignSalesType, seasonality, skuBrandType, salesQuantity, salesCurr,
                    gm, discountTtl, turnoverCurr, skuSeasonBudget, storeRusBpo,
                    salesChannelBpo, mfpSubDepartment, skuTm, mfpNode, section,
                    merchandiseSubGroup, skuPhase, skuProductClass
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        long startedAt = System.nanoTime();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (SalesByChannelStageRow row : rows) {
                if (row.loadSessionId() != loadSessionId) {
                    throw new IllegalArgumentException(
                            "SalesByChannel stage row belongs to another load session. expected="
                                    + loadSessionId + ", actual=" + row.loadSessionId()
                    );
                }
                bind(ps, row);
                ps.addBatch();
            }
            int[] counts = ps.executeBatch();
            validateCounts(counts, rows.size(), loadSessionId);
            ps.clearBatch();
            log.info("SalesByChannel stage chunk inserted. loadSessionId={}, chunkSize={}, elapsedMs={}, "
                            + "updateCountsLength={}",
                    loadSessionId, rows.size(), elapsedMs(startedAt), counts.length);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to insert SalesByChannel_stage rows. loadSessionId=" + loadSessionId, e
            );
        }
    }

    private void bind(PreparedStatement ps, SalesByChannelStageRow row) throws SQLException {
        ps.setLong(1, row.loadSessionId());
        nullableLong(ps, 2, row.excelRowNum());
        nullableString(ps, 3, row.seasonYear());
        nullableString(ps, 4, row.season6m());
        nullableString(ps, 5, row.yearMonth());
        nullableString(ps, 6, row.yearSeason());
        ps.setString(7, row.year());
        ps.setString(8, row.month());
        nullableString(ps, 9, row.salesChannelType());
        nullableString(ps, 10, row.storeRus());
        nullableString(ps, 11, row.typeOfSales());
        nullableString(ps, 12, row.mfpDivision());
        nullableString(ps, 13, row.mfpDepartment());
        nullableString(ps, 14, row.campaignSalesType());
        nullableString(ps, 15, row.seasonality());
        nullableString(ps, 16, row.skuBrandType());
        ps.setInt(17, row.salesQuantity());
        ps.setBigDecimal(18, row.salesCurr());
        ps.setBigDecimal(19, row.gm());
        ps.setBigDecimal(20, row.discountTtl());
        ps.setBigDecimal(21, row.turnoverCurr());
        nullableString(ps, 22, row.skuSeasonBudget());
        nullableString(ps, 23, row.storeRusBpo());
        nullableString(ps, 24, row.salesChannelBpo());
        nullableString(ps, 25, row.mfpSubDepartment());
        nullableString(ps, 26, row.skuTm());
        nullableString(ps, 27, row.mfpNode());
        nullableString(ps, 28, row.section());
        nullableString(ps, 29, row.merchandiseSubGroup());
        nullableString(ps, 30, row.skuPhase());
        nullableString(ps, 31, row.skuProductClass());
    }

    private void validateCounts(int[] counts, int expected, long loadSessionId) {
        if (counts == null || counts.length != expected) {
            throw new IllegalStateException(
                    "Unexpected SalesByChannel stage update counts. loadSessionId=" + loadSessionId
                            + ", expected=" + expected
                            + ", actual=" + (counts == null ? "null" : counts.length)
            );
        }
        for (int count : counts) {
            if (count == Statement.EXECUTE_FAILED) {
                throw new IllegalStateException(
                        "SalesByChannel stage batch contains failed statement. loadSessionId=" + loadSessionId
                );
            }
        }
    }

    private void nullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) ps.setNull(index, Types.NVARCHAR); else ps.setString(index, value);
    }

    private void nullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) ps.setNull(index, Types.BIGINT); else ps.setLong(index, value);
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
