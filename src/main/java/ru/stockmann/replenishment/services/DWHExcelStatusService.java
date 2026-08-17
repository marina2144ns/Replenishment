package ru.stockmann.replenishment.services;

import org.springframework.stereotype.Service;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadSessionNotFoundException;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadStatusResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Service
public class DWHExcelStatusService {

    private final DataSource dataSource;

    public DWHExcelStatusService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DWHExcelLoadStatusResult getStatus(Long loadSessionId) {
        if (loadSessionId == null) {
            throw new IllegalArgumentException("loadSessionId is required");
        }

        String sql = """
                SELECT
                    Id,
                    LoadTypeCode,
                    ServiceName,
                    FileName,
                    FilePath,
                    OperationType,
                    OperationMode,
                    DeleteYear,
                    DeleteWeek,
                    DeleteMonth,
                    DeleteYearText,
                    DeleteMonthText,
                    SourceLoadSessionId,
                    DeleteCriterion,
                    DeleteParameter1Name,
                    DeleteParameter1Value,
                    DeleteParameter2Name,
                    DeleteParameter2Value,
                    DeletedRows,
                    Status,
                    Message,
                    StartedAt,
                    FinishedAt
                FROM dbo.DWH_Excel_Load_Session
                WHERE Id = ?
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, loadSessionId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new DWHExcelLoadSessionNotFoundException(loadSessionId);
                }

                return new DWHExcelLoadStatusResult(
                        rs.getLong("Id"),
                        rs.getString("LoadTypeCode"),
                        rs.getString("ServiceName"),
                        rs.getString("FileName"),
                        rs.getString("FilePath"),
                        rs.getString("OperationType"),
                        rs.getString("OperationMode"),
                        getNullableInteger(rs, "DeleteYear"),
                        getNullableInteger(rs, "DeleteWeek"),
                        getNullableInteger(rs, "DeleteMonth"),
                        rs.getString("DeleteYearText"),
                        rs.getString("DeleteMonthText"),
                        getNullableLong(rs, "SourceLoadSessionId"),
                        rs.getString("DeleteCriterion"),
                        rs.getString("DeleteParameter1Name"),
                        rs.getString("DeleteParameter1Value"),
                        rs.getString("DeleteParameter2Name"),
                        rs.getString("DeleteParameter2Value"),
                        getNullableLong(rs, "DeletedRows"),
                        rs.getString("Status"),
                        rs.getString("Message"),
                        toStringOrNull(rs.getTimestamp("StartedAt")),
                        toStringOrNull(rs.getTimestamp("FinishedAt"))
                );
            }

        } catch (IllegalArgumentException | DWHExcelLoadSessionNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get load session status: " + loadSessionId, e);
        }
    }

    private String toStringOrNull(Timestamp timestamp) {
        return timestamp != null
                ? timestamp.toLocalDateTime().toString()
                : null;
    }

    private Integer getNullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
