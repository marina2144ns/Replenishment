package ru.stockmann.replenishment.services.cdecom.process;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Objects;

public class CDEcomTargetRepository {

    private final DataSource dataSource;

    public CDEcomTargetRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void deleteByLoadSessionId(Connection connection, long loadSessionId) {
        String sql = """
                DELETE FROM dbo.CD_ecom
                WHERE LoadSessionId = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete CD_ecom rows. loadSessionId=" + loadSessionId, e);
        }
    }

    public void insertAll(Connection connection, List<CDEcomTargetRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO dbo.CD_ecom
                (
                    LoadSessionId,
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
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (CDEcomTargetRow row : rows) {
                bindRow(ps, row);
                ps.addBatch();
            }

            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert CD_ecom rows", e);
        }
    }

    private void bindRow(PreparedStatement ps, CDEcomTargetRow row) throws SQLException {
        Long loadSessionId = Objects.requireNonNull(row.loadSessionId(), "CDEcom target LoadSessionId is required");
        ps.setLong(1, loadSessionId);
        setNullableString(ps, 2, row.name());
        setNullableInteger(ps, 3, row.year());
        setNullableInteger(ps, 4, row.season());
        setNullableInteger(ps, 5, row.day());
        setNullableDate(ps, 6, row.data());
        setNullableString(ps, 7, row.salesChannelBpo());
        setNullableString(ps, 8, row.storeRus());
        setNullableString(ps, 9, row.mfpDivision());
        setNullableString(ps, 10, row.mfpDepartment());
        setNullableString(ps, 11, row.mfpSubDepartment());
        setNullableString(ps, 12, row.skuBrandType());
        setNullableString(ps, 13, row.skuTm());
        setNullableString(ps, 14, row.mfpNode());
        setNullableString(ps, 15, row.section());
        setNullableString(ps, 16, row.merchandiseSubGroup());
        setNullableString(ps, 17, row.campaignSalesType());
        setNullableLong(ps, 18, row.skuStyleColor());
        setNullableString(ps, 19, row.skuPhase());
        setNullableDecimal(ps, 20, row.orderPcs());
        setNullableDecimal(ps, 21, row.orderRub());
        setNullableDecimal(ps, 22, row.foundPcs());
        setNullableDecimal(ps, 23, row.foundRub());
        setNullableDecimal(ps, 24, row.salesPcs());
        setNullableDecimal(ps, 25, row.salesRub());
        setNullableDecimal(ps, 26, row.revenue());
        setNullableDecimal(ps, 27, row.gp());
        setNullableDecimal(ps, 28, row.cogs());
        setNullableDecimal(ps, 29, row.salesDiscount());
        setNullableLong(ps, 30, row.planRub());
        setNullableLong(ps, 31, row.stockStoresPcs());
        setNullableLong(ps, 32, row.stockStoresDdp());
        setNullableString(ps, 33, row.cdDrivers());
        setNullableString(ps, 34, row.skuSupplierModel());
        setNullableString(ps, 35, row.skuComposition());
        setNullableString(ps, 36, row.skuColorRussian());
        setNullableString(ps, 37, row.skuName());
        setNullableString(ps, 38, row.skuCommentBuyer());
        setNullableString(ps, 39, row.skuCollection());
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
