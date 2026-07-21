package ru.stockmann.replenishment.services.cdecom.process;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

public class CDEcomErrorRepository {

    private static final String LOAD_TYPE_CODE = "CD_ECOM";

    private final DataSource dataSource;

    public CDEcomErrorRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void deleteByLoadSessionId(Connection connection, long loadSessionId) {
        String sql = """
                DELETE FROM dbo.DWH_Excel_Load_Error
                WHERE LoadSessionId = ?
                  AND LoadTypeCode = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            ps.setString(2, LOAD_TYPE_CODE);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete CDEcom load errors. loadSessionId=" + loadSessionId, e);
        }
    }

    public void insertAll(List<CDEcomValidationError> errors) {
        if (errors == null || errors.isEmpty()) {
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            insertAll(connection, errors);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert CDEcom load errors", e);
        }
    }

    public void insertAll(Connection connection, List<CDEcomValidationError> errors) {
        if (errors == null || errors.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO dbo.DWH_Excel_Load_Error
                (
                    LoadSessionId,
                    LoadTypeCode,
                    ErrorLayer,
                    ExcelRowNum,
                    RawId,
                    FieldName,
                    ErrorCode,
                    ErrorReason,
                    ErrorMessage
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (CDEcomValidationError error : errors) {
                ps.setLong(1, error.loadSessionId());
                ps.setString(2, LOAD_TYPE_CODE);
                ps.setString(3, error.errorLayer());
                setNullableBigInt(ps, 4, error.excelRowNum());
                setNullableBigInt(ps, 5, error.rawId());
                setNullableString(ps, 6, error.fieldName());
                setNullableString(ps, 7, error.errorCode());
                setNullableString(ps, 8, error.errorReason());
                ps.setString(9, error.errorMessage());
                ps.addBatch();
            }

            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert CDEcom load errors", e);
        }
    }

    private void setNullableBigInt(PreparedStatement ps, int parameterIndex, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(parameterIndex, Types.BIGINT);
        } else {
            ps.setLong(parameterIndex, value);
        }
    }

    private void setNullableString(PreparedStatement ps, int parameterIndex, String value) throws SQLException {
        if (value == null) {
            ps.setNull(parameterIndex, Types.NVARCHAR);
        } else {
            ps.setString(parameterIndex, value);
        }
    }
}
