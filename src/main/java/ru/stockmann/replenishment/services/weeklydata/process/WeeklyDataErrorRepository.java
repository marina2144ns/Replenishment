package ru.stockmann.replenishment.services.weeklydata.process;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class WeeklyDataErrorRepository {

    private static final String LOAD_TYPE_CODE = "WEEKLY_DATA";

    private final DataSource dataSource;

    public WeeklyDataErrorRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void deleteByLoadSessionId(long loadSessionId) {
        try (Connection connection = dataSource.getConnection()) {
            deleteByLoadSessionId(connection, loadSessionId);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to delete WeeklyData load errors. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    public void deleteByLoadSessionId(Connection connection, long loadSessionId) {
        String sql = """
                DELETE FROM dbo.DWH_Excel_Load_Error
                WHERE LoadSessionId = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, loadSessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to delete WeeklyData load errors. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    public void insertAll(List<WeeklyDataValidationError> errors) {
        try (Connection connection = dataSource.getConnection()) {
            insertAll(connection, errors);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert WeeklyData load errors", e);
        }
    }

    public void insertAll(Connection connection, List<WeeklyDataValidationError> errors) {
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

            for (WeeklyDataValidationError error : errors) {
                ps.setLong(1, error.loadSessionId());
                ps.setString(2, LOAD_TYPE_CODE);
                ps.setString(3, error.errorLayer());
                setNullableBigInt(ps, 4, error.excelRowNum());
                ps.setLong(5, error.rawId());
                ps.setString(6, error.fieldName());
                ps.setString(7, error.errorCode());
                ps.setString(8, error.errorReason());
                ps.setString(9, error.errorMessage());
                ps.addBatch();
            }

            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert WeeklyData load errors", e);
        }
    }

    private void setNullableBigInt(PreparedStatement ps, int parameterIndex, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(parameterIndex, java.sql.Types.BIGINT);
        } else {
            ps.setLong(parameterIndex, value);
        }
    }
}
