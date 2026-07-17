package ru.stockmann.replenishment.services.cddata.process;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CDDataRawRepository {

    private final DataSource dataSource;

    public CDDataRawRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<CDDataRawRow> findByLoadSessionId(long loadSessionId) {
        try (Connection connection = dataSource.getConnection()) {
            return findByLoadSessionId(connection, loadSessionId);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to read CD_data_raw rows. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    public List<CDDataRawRow> findByLoadSessionId(Connection connection, long loadSessionId) {
        String sql = """
                SELECT
                    Id,
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
                FROM dbo.CD_data_raw
                WHERE LoadSessionId = ?
                ORDER BY Id ASC
                """;

        List<CDDataRawRow> rows = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapRow(rs));
                }
            }

            return rows;
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to read CD_data_raw rows. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    private CDDataRawRow mapRow(ResultSet rs) throws SQLException {
        return new CDDataRawRow(
                rs.getLong("Id"),
                rs.getLong("LoadSessionId"),
                getNullableLong(rs, "ExcelRowNum"),
                rs.getString("nazvanie"),
                rs.getString("god"),
                rs.getString("sezon"),
                rs.getString("den"),
                rs.getString("data"),
                rs.getString("sales_channel"),
                rs.getString("store_rus"),
                rs.getString("mfp_division"),
                rs.getString("mfp_department"),
                rs.getString("mfp_sub_department"),
                rs.getString("sku_brand_type"),
                rs.getString("sku_tm"),
                rs.getString("mfp_node"),
                rs.getString("section"),
                rs.getString("merchandise_sub_group"),
                rs.getString("campaign_sales"),
                rs.getString("sku_style_color"),
                rs.getString("sku_phase"),
                rs.getString("stock_start_pcs"),
                rs.getString("stock_start_dd"),
                rs.getString("sales_pcs"),
                rs.getString("sales_rub"),
                rs.getString("revenue"),
                rs.getString("gp"),
                rs.getString("cogs"),
                rs.getString("sales_frp_price"),
                rs.getString("sales_discount"),
                rs.getString("stock_stores_pcs"),
                rs.getString("stock_stores_dd"),
                rs.getString("plan_rub"),
                rs.getString("draivery_cd"),
                rs.getString("sku_color_rus"),
                rs.getString("sku_composition"),
                rs.getString("sku_supplier"),
                rs.getString("sku_name"),
                rs.getString("sku_collection"),
                rs.getString("sku_comment")
        );
    }

    private Long getNullableLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }
}
