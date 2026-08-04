package ru.stockmann.replenishment.services.cdecom.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;

public class CDEcomStageRepository {

    private static final Logger log = LoggerFactory.getLogger(CDEcomStageRepository.class);

    public int deleteByLoadSessionId(Connection connection, long loadSessionId) {
        String sql = """
                DELETE FROM dbo.CD_ecom_stage
                WHERE LoadSessionId = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to delete CD_ecom_stage rows. loadSessionId=" + loadSessionId, e
            );
        }
    }

    public void insertBatch(Connection connection, long loadSessionId, List<CDEcomStageRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO dbo.CD_ecom_stage
                (
                    LoadSessionId,
                    ExcelRowNum,
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
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        long startedAt = System.nanoTime();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (CDEcomStageRow row : rows) {
                if (row.loadSessionId() == null || row.loadSessionId() != loadSessionId) {
                    throw new IllegalArgumentException(
                            "CDEcom stage row belongs to another load session. expected=" + loadSessionId
                                    + ", actual=" + row.loadSessionId()
                    );
                }
                bindRow(ps, row);
                ps.addBatch();
            }
            int[] updateCounts = ps.executeBatch();
            validateUpdateCounts(updateCounts, rows.size(), loadSessionId);
            ps.clearBatch();
            log.info("CDEcom stage chunk inserted. loadSessionId={}, chunkSize={}, elapsedMs={}, "
                            + "updateCountsLength={}",
                    loadSessionId, rows.size(), elapsedMs(startedAt), updateCounts.length);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to insert CD_ecom_stage rows. loadSessionId=" + loadSessionId, e
            );
        }
    }

    private void bindRow(PreparedStatement ps, CDEcomStageRow row) throws SQLException {
        ps.setLong(1, row.loadSessionId());
        setNullableLong(ps, 2, row.excelRowNum());
        setNullableString(ps, 3, row.name());
        setNullableInteger(ps, 4, row.year());
        setNullableInteger(ps, 5, row.season());
        setNullableInteger(ps, 6, row.day());
        setNullableDate(ps, 7, row.data());
        setNullableString(ps, 8, row.salesChannelBpo());
        setNullableString(ps, 9, row.storeRus());
        setNullableString(ps, 10, row.mfpDivision());
        setNullableString(ps, 11, row.mfpDepartment());
        setNullableString(ps, 12, row.mfpSubDepartment());
        setNullableString(ps, 13, row.skuBrandType());
        setNullableString(ps, 14, row.skuTm());
        setNullableString(ps, 15, row.mfpNode());
        setNullableString(ps, 16, row.section());
        setNullableString(ps, 17, row.merchandiseSubGroup());
        setNullableString(ps, 18, row.campaignSalesType());
        setNullableLong(ps, 19, row.skuStyleColor());
        setNullableString(ps, 20, row.skuPhase());
        setNullableDecimal(ps, 21, row.orderPcs());
        setNullableDecimal(ps, 22, row.orderRub());
        setNullableDecimal(ps, 23, row.foundPcs());
        setNullableDecimal(ps, 24, row.foundRub());
        setNullableDecimal(ps, 25, row.salesPcs());
        setNullableDecimal(ps, 26, row.salesRub());
        setNullableDecimal(ps, 27, row.revenue());
        setNullableDecimal(ps, 28, row.gp());
        setNullableDecimal(ps, 29, row.cogs());
        setNullableDecimal(ps, 30, row.salesDiscount());
        setNullableLong(ps, 31, row.planRub());
        setNullableLong(ps, 32, row.stockStoresPcs());
        setNullableLong(ps, 33, row.stockStoresDdp());
        setNullableString(ps, 34, row.cdDrivers());
        setNullableString(ps, 35, row.skuSupplierModel());
        setNullableString(ps, 36, row.skuComposition());
        setNullableString(ps, 37, row.skuColorRussian());
        setNullableString(ps, 38, row.skuName());
        setNullableString(ps, 39, row.skuCommentBuyer());
        setNullableString(ps, 40, row.skuCollection());
        setNullableLong(ps, 41, row.rawRowId());
    }

    private void validateUpdateCounts(int[] updateCounts, int expected, long loadSessionId) {
        if (updateCounts == null || updateCounts.length != expected) {
            throw new IllegalStateException(
                    "Unexpected CD_ecom_stage update counts. loadSessionId=" + loadSessionId
                            + ", expected=" + expected
                            + ", actual=" + (updateCounts == null ? "null" : updateCounts.length)
            );
        }
        for (int updateCount : updateCounts) {
            if (updateCount == Statement.EXECUTE_FAILED) {
                throw new IllegalStateException(
                        "CD_ecom_stage batch contains failed statement. loadSessionId=" + loadSessionId
                );
            }
        }
    }

    private void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) ps.setNull(index, Types.NVARCHAR); else ps.setString(index, value);
    }

    private void setNullableInteger(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) ps.setNull(index, Types.INTEGER); else ps.setInt(index, value);
    }

    private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) ps.setNull(index, Types.BIGINT); else ps.setLong(index, value);
    }

    private void setNullableDecimal(PreparedStatement ps, int index, BigDecimal value) throws SQLException {
        if (value == null) ps.setNull(index, Types.DECIMAL); else ps.setBigDecimal(index, value);
    }

    private void setNullableDate(PreparedStatement ps, int index, LocalDate value) throws SQLException {
        if (value == null) ps.setNull(index, Types.DATE); else ps.setObject(index, value, Types.DATE);
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
