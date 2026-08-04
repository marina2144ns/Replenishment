package ru.stockmann.replenishment.services.cddata.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CDDataTargetRepository {

    private static final Logger log = LoggerFactory.getLogger(CDDataTargetRepository.class);

    public int publishFromStage(Connection connection, long loadSessionId) {
        deleteTargetRows(connection, loadSessionId);
        return insertFromStage(connection, loadSessionId);
    }

    private void deleteTargetRows(Connection connection, long loadSessionId) {
        String sql = """
                DELETE FROM dbo.CD_data
                WHERE LoadSessionId = ?
                """;

        long startedAt = System.nanoTime();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            int affectedRows = ps.executeUpdate();
            log.info("CDData target delete completed. loadSessionId={}, affectedRows={}, elapsedMs={}",
                    loadSessionId, affectedRows, elapsedMs(startedAt));
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to delete CD_data rows. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    private int insertFromStage(Connection connection, long loadSessionId) {
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
                    sku_comment,
                    RawRowId
                )
                SELECT
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
                    sku_comment,
                    RawRowId
                FROM dbo.CD_data_stage
                WHERE LoadSessionId = ?
                """;

        long startedAt = System.nanoTime();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            int publishedRows = ps.executeUpdate();
            log.info("CDData stage INSERT SELECT completed. loadSessionId={}, publishedRows={}, elapsedMs={}",
                    loadSessionId, publishedRows, elapsedMs(startedAt));
            return publishedRows;
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to publish CD_data_stage rows. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
