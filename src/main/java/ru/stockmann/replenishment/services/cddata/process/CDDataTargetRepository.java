package ru.stockmann.replenishment.services.cddata.process;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

public class CDDataTargetRepository {

    private final DataSource dataSource;

    public CDDataTargetRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void deleteByLoadSessionId(long loadSessionId) {
        try (Connection connection = dataSource.getConnection()) {
            deleteByLoadSessionId(connection, loadSessionId);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to delete CD_data rows. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    public void deleteByLoadSessionId(Connection connection, long loadSessionId) {
        String sql = """
                DELETE FROM dbo.CD_data
                WHERE LoadSessionId = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to delete CD_data rows. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    public void insertAll(List<CDDataTargetRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            insertAll(connection, rows);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert CD_data rows", e);
        }
    }

    public void insertAll(Connection connection, List<CDDataTargetRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO dbo.CD_data
                (
                    LoadSessionId,
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
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (CDDataTargetRow row : rows) {
                bindRow(ps, row);
                ps.addBatch();
            }

            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert CD_data rows", e);
        }
    }

    private void bindRow(PreparedStatement ps, CDDataTargetRow row) throws SQLException {
        ps.setLong(1, row.loadSessionId());
        setNullableString(ps, 2, row.nazvanie());
        setNullableInteger(ps, 3, row.god());
        setNullableInteger(ps, 4, row.sezon());
        setNullableInteger(ps, 5, row.den());
        setNullableDate(ps, 6, row.data());
        setNullableString(ps, 7, row.salesChannel());
        setNullableString(ps, 8, row.storeRus());
        setNullableString(ps, 9, row.mfpDivision());
        setNullableString(ps, 10, row.mfpDepartment());
        setNullableString(ps, 11, row.mfpSubDepartment());
        setNullableString(ps, 12, row.skuBrandType());
        setNullableString(ps, 13, row.skuTm());
        setNullableString(ps, 14, row.mfpNode());
        setNullableString(ps, 15, row.section());
        setNullableString(ps, 16, row.merchandiseSubGroup());
        setNullableString(ps, 17, row.campaignSales());
        setNullableLong(ps, 18, row.skuStyleColor());
        setNullableString(ps, 19, row.skuPhase());
        setNullableDecimal(ps, 20, row.stockStartPcs());
        setNullableDecimal(ps, 21, row.stockStartDd());
        setNullableDecimal(ps, 22, row.salesPcs());
        setNullableDecimal(ps, 23, row.salesRub());
        setNullableDecimal(ps, 24, row.revenue());
        setNullableDecimal(ps, 25, row.gp());
        setNullableDecimal(ps, 26, row.cogs());
        setNullableDecimal(ps, 27, row.salesFrpPrice());
        setNullableDecimal(ps, 28, row.salesDiscount());
        setNullableDecimal(ps, 29, row.stockStoresPcs());
        setNullableDecimal(ps, 30, row.stockStoresDd());
        setNullableInteger(ps, 31, row.planRub());
        setNullableString(ps, 32, row.draiveryCd());
        setNullableString(ps, 33, row.skuColorRus());
        setNullableString(ps, 34, row.skuComposition());
        setNullableString(ps, 35, row.skuSupplier());
        setNullableString(ps, 36, row.skuName());
        setNullableString(ps, 37, row.skuCollection());
        setNullableString(ps, 38, row.skuComment());
    }

    private void setNullableString(PreparedStatement ps, int parameterIndex, String value) throws SQLException {
        if (value == null) {
            ps.setNull(parameterIndex, Types.NVARCHAR);
        } else {
            ps.setString(parameterIndex, value);
        }
    }

    private void setNullableInteger(PreparedStatement ps, int parameterIndex, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(parameterIndex, Types.INTEGER);
        } else {
            ps.setInt(parameterIndex, value);
        }
    }

    private void setNullableLong(PreparedStatement ps, int parameterIndex, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(parameterIndex, Types.BIGINT);
        } else {
            ps.setLong(parameterIndex, value);
        }
    }

    private void setNullableDecimal(PreparedStatement ps, int parameterIndex, BigDecimal value) throws SQLException {
        if (value == null) {
            ps.setNull(parameterIndex, Types.DECIMAL);
        } else {
            ps.setBigDecimal(parameterIndex, value);
        }
    }

    private void setNullableDate(PreparedStatement ps, int parameterIndex, Date value) throws SQLException {
        if (value == null) {
            ps.setNull(parameterIndex, Types.DATE);
        } else {
            ps.setDate(parameterIndex, value);
        }
    }
}
