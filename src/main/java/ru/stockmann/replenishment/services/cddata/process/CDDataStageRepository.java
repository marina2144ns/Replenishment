package ru.stockmann.replenishment.services.cddata.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

public class CDDataStageRepository {

    private static final Logger log = LoggerFactory.getLogger(CDDataStageRepository.class);

    public int deleteByLoadSessionId(Connection connection, long loadSessionId) {
        String sql = """
                DELETE FROM dbo.CD_data_stage
                WHERE LoadSessionId = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to delete CD_data_stage rows. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    public void insertBatch(Connection connection, long loadSessionId, List<CDDataStageRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO dbo.CD_data_stage
                (
                    LoadSessionId,
                    ExcelRowNum,
                    nazvanie,
                    god,
                    sezon,
                    den,
                    data,
                    sales_channel,
                    store_rus,
                    mfp_division,
                    mfp_department,
                    mfp_sub_department,
                    sku_brand_type,
                    sku_tm,
                    mfp_node,
                    section,
                    merchandise_sub_group,
                    campaign_sales,
                    sku_style_color,
                    sku_phase,
                    stock_start_pcs,
                    stock_start_dd,
                    sales_pcs,
                    sales_rub,
                    revenue,
                    gp,
                    cogs,
                    sales_frp_price,
                    sales_discount,
                    stock_stores_pcs,
                    stock_stores_dd,
                    plan_rub,
                    draivery_cd,
                    sku_color_rus,
                    sku_composition,
                    sku_supplier,
                    sku_name,
                    sku_collection,
                    sku_comment
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        long startedAt = System.nanoTime();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (CDDataStageRow row : rows) {
                if (row.loadSessionId() != loadSessionId) {
                    throw new IllegalArgumentException(
                            "CDData stage row belongs to another load session. expected=" + loadSessionId
                                    + ", actual=" + row.loadSessionId()
                    );
                }
                bindRow(ps, row);
                ps.addBatch();
            }
            int[] updateCounts = ps.executeBatch();
            validateUpdateCounts(updateCounts, rows.size(), loadSessionId);
            ps.clearBatch();
            log.info("CDData stage chunk inserted. loadSessionId={}, chunkSize={}, elapsedMs={}, "
                            + "updateCountsLength={}",
                    loadSessionId, rows.size(), elapsedMs(startedAt), updateCounts.length);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to insert CD_data_stage rows. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    private void bindRow(PreparedStatement ps, CDDataStageRow row) throws SQLException {
        ps.setLong(1, row.loadSessionId());
        setNullableLong(ps, 2, row.excelRowNum());
        setNullableString(ps, 3, row.nazvanie());
        setNullableInteger(ps, 4, row.god());
        setNullableInteger(ps, 5, row.sezon());
        setNullableInteger(ps, 6, row.den());
        setNullableDate(ps, 7, row.data());
        setNullableString(ps, 8, row.salesChannel());
        setNullableString(ps, 9, row.storeRus());
        setNullableString(ps, 10, row.mfpDivision());
        setNullableString(ps, 11, row.mfpDepartment());
        setNullableString(ps, 12, row.mfpSubDepartment());
        setNullableString(ps, 13, row.skuBrandType());
        setNullableString(ps, 14, row.skuTm());
        setNullableString(ps, 15, row.mfpNode());
        setNullableString(ps, 16, row.section());
        setNullableString(ps, 17, row.merchandiseSubGroup());
        setNullableString(ps, 18, row.campaignSales());
        setNullableLong(ps, 19, row.skuStyleColor());
        setNullableString(ps, 20, row.skuPhase());
        setNullableDecimal(ps, 21, row.stockStartPcs());
        setNullableDecimal(ps, 22, row.stockStartDd());
        setNullableDecimal(ps, 23, row.salesPcs());
        setNullableDecimal(ps, 24, row.salesRub());
        setNullableDecimal(ps, 25, row.revenue());
        setNullableDecimal(ps, 26, row.gp());
        setNullableDecimal(ps, 27, row.cogs());
        setNullableDecimal(ps, 28, row.salesFrpPrice());
        setNullableDecimal(ps, 29, row.salesDiscount());
        setNullableDecimal(ps, 30, row.stockStoresPcs());
        setNullableDecimal(ps, 31, row.stockStoresDd());
        setNullableInteger(ps, 32, row.planRub());
        setNullableString(ps, 33, row.draiveryCd());
        setNullableString(ps, 34, row.skuColorRus());
        setNullableString(ps, 35, row.skuComposition());
        setNullableString(ps, 36, row.skuSupplier());
        setNullableString(ps, 37, row.skuName());
        setNullableString(ps, 38, row.skuCollection());
        setNullableString(ps, 39, row.skuComment());
    }

    private void validateUpdateCounts(int[] updateCounts, int expected, long loadSessionId) {
        if (updateCounts == null || updateCounts.length != expected) {
            throw new IllegalStateException(
                    "Unexpected CD_data_stage update counts. loadSessionId=" + loadSessionId
                            + ", expected=" + expected
                            + ", actual=" + (updateCounts == null ? "null" : updateCounts.length)
            );
        }
        for (int updateCount : updateCounts) {
            if (updateCount == Statement.EXECUTE_FAILED) {
                throw new IllegalStateException(
                        "CD_data_stage batch contains failed statement. loadSessionId=" + loadSessionId
                );
            }
        }
    }

    private void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.NVARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    private void setNullableInteger(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    private void setNullableDecimal(PreparedStatement ps, int index, BigDecimal value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.DECIMAL);
        } else {
            ps.setBigDecimal(index, value);
        }
    }

    private void setNullableDate(PreparedStatement ps, int index, Date value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setDate(index, value);
        }
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
