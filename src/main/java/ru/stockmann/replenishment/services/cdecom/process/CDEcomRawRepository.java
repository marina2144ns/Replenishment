package ru.stockmann.replenishment.services.cdecom.process;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CDEcomRawRepository {

    public static final int DEFAULT_CHUNK_SIZE = 1_000;
    public static final long INITIAL_LAST_RAW_ID = 0L;

    private final DataSource dataSource;

    public CDEcomRawRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<CDEcomRawRow> findByLoadSessionId(long loadSessionId) {
        try (Connection connection = dataSource.getConnection()) {
            return findByLoadSessionId(connection, loadSessionId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read CD_ecom_raw rows. loadSessionId=" + loadSessionId, e);
        }
    }

    public List<CDEcomRawRow> findByLoadSessionId(Connection connection, long loadSessionId) {
        String sql = """
                SELECT
                    Id,
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
                    skuCollection
                FROM dbo.CD_ecom_raw
                WHERE LoadSessionId = ?
                ORDER BY Id ASC
                """;

        List<CDEcomRawRow> rows = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapRow(rs));
                }
            }

            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read CD_ecom_raw rows. loadSessionId=" + loadSessionId, e);
        }
    }

    public List<CDEcomRawRow> findChunk(long loadSessionId, long lastRawId) {
        try (Connection connection = dataSource.getConnection()) {
            return findChunk(connection, loadSessionId, lastRawId, DEFAULT_CHUNK_SIZE);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read CD_ecom_raw chunk. loadSessionId=" + loadSessionId, e);
        }
    }

    public List<CDEcomRawRow> findChunk(
            Connection connection,
            long loadSessionId,
            long lastRawId,
            int chunkSize
    ) {
        String sql = """
                SELECT TOP (?)
                    Id,
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
                    skuCollection
                FROM dbo.CD_ecom_raw
                WHERE LoadSessionId = ?
                  AND Id > ?
                ORDER BY Id
                """;

        List<CDEcomRawRow> rows = new ArrayList<>(chunkSize);
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
            throw new RuntimeException("Failed to read CD_ecom_raw chunk. loadSessionId=" + loadSessionId, e);
        }
    }

    private CDEcomRawRow mapRow(ResultSet rs) throws SQLException {
        return new CDEcomRawRow(
                rs.getLong("Id"),
                rs.getLong("LoadSessionId"),
                getNullableLong(rs, "ExcelRowNum"),
                rs.getString("name"),
                rs.getString("year"),
                rs.getString("season"),
                rs.getString("day"),
                rs.getString("data"),
                rs.getString("salesChannelBpo"),
                rs.getString("storeRus"),
                rs.getString("mfpDivision"),
                rs.getString("mfpDepartment"),
                rs.getString("mfpSubDepartment"),
                rs.getString("skuBrandType"),
                rs.getString("skuTm"),
                rs.getString("mfpNode"),
                rs.getString("section"),
                rs.getString("merchandiseSubGroup"),
                rs.getString("campaignSalesType"),
                rs.getString("skuStyleColor"),
                rs.getString("skuPhase"),
                rs.getString("orderPcs"),
                rs.getString("orderRub"),
                rs.getString("foundPcs"),
                rs.getString("foundRub"),
                rs.getString("salesPcs"),
                rs.getString("salesRub"),
                rs.getString("revenue"),
                rs.getString("gp"),
                rs.getString("cogs"),
                rs.getString("salesDiscount"),
                rs.getString("planRub"),
                rs.getString("stockStoresPcs"),
                rs.getString("stockStoresDdp"),
                rs.getString("cdDrivers"),
                rs.getString("skuSupplierModel"),
                rs.getString("skuComposition"),
                rs.getString("skuColorRussian"),
                rs.getString("skuName"),
                rs.getString("skuCommentBuyer"),
                rs.getString("skuCollection")
        );
    }

    private Long getNullableLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }
}
