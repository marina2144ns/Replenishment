package ru.stockmann.replenishment.services.dwhexcelload.core;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DWHDeletionSessionRepository {

    private static final String DELETE_OPERATION = "DELETE";

    private final DataSource dataSource;

    public DWHDeletionSessionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public long create(DWHDeletionSession session) {
        String sql = """
                INSERT INTO dbo.DWH_Excel_Load_Session
                (
                    LoadTypeCode,
                    ServiceName,
                    OperationType,
                    OperationMode,
                    DeleteYear,
                    DeleteWeek,
                    DeleteMonth,
                    SourceLoadSessionId,
                    DeleteCriterion,
                    DeleteParameter1Name,
                    DeleteParameter1Value,
                    DeleteParameter2Name,
                    DeleteParameter2Value,
                    Status
                )
                OUTPUT INSERTED.Id
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, session.loadType().code());
            ps.setString(2, session.loadType().displayName());
            ps.setString(3, DELETE_OPERATION);
            ps.setString(4, session.operationMode().name());
            setNullableInteger(ps, 5, session.deleteYear());
            setNullableInteger(ps, 6, session.deleteWeek());
            setNullableInteger(ps, 7, session.deleteMonth());
            setNullableLong(ps, 8, session.sourceLoadSessionId());
            setNullableString(ps, 9, session.deleteCriterion());
            setNullableString(ps, 10, session.deleteParameter1Name());
            setNullableString(ps, 11, session.deleteParameter1Value());
            setNullableString(ps, 12, session.deleteParameter2Name());
            setNullableString(ps, 13, session.deleteParameter2Value());
            ps.setString(14, DWHExcelLoadStatus.RUNNING.name());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Deletion session INSERT returned no Id");
                }
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create deletion session", e);
        }
    }

    public void completeSuccess(Connection connection, long sessionId, long deletedRows) {
        String sql = """
                UPDATE dbo.DWH_Excel_Load_Session
                SET Status = ?,
                    DeletedRows = ?,
                    FinishedAt = SYSDATETIME(),
                    Message = NULL
                WHERE Id = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, DWHExcelLoadStatus.SUCCESS.name());
            ps.setLong(2, deletedRows);
            ps.setLong(3, sessionId);
            requireSingleUpdate(ps.executeUpdate(), sessionId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to complete deletion session. sessionId=" + sessionId, e);
        }
    }

    public void completeError(long sessionId, String message) {
        String sql = """
                UPDATE dbo.DWH_Excel_Load_Session
                SET Status = ?,
                    FinishedAt = SYSDATETIME(),
                    Message = ?
                WHERE Id = ?
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, DWHExcelLoadStatus.ERROR.name());
            ps.setString(2, truncate(message, 2000));
            ps.setLong(3, sessionId);
            requireSingleUpdate(ps.executeUpdate(), sessionId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to mark deletion session as ERROR. sessionId=" + sessionId, e);
        }
    }

    private void setNullableInteger(PreparedStatement ps, int index, Integer value)
            throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private void setNullableLong(PreparedStatement ps, int index, Long value)
            throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    private void setNullableString(PreparedStatement ps, int index, String value)
            throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.NVARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    private void requireSingleUpdate(int updatedRows, long sessionId) {
        if (updatedRows != 1) {
            throw new IllegalStateException(
                    "Deletion session update affected " + updatedRows + " rows. sessionId=" + sessionId
            );
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
