package ru.stockmann.replenishment.services.dwhexcelload.core;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

@Service
public class DWHExcelLoadStatusService {

    private final DataSource dataSource;

    public DWHExcelLoadStatusService(DataSource dataSource) {
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
                    throw new IllegalArgumentException("Load session not found: " + loadSessionId);
                }

                return new DWHExcelLoadStatusResult(
                        rs.getLong("Id"),
                        rs.getString("LoadTypeCode"),
                        rs.getString("ServiceName"),
                        rs.getString("FileName"),
                        rs.getString("FilePath"),
                        rs.getString("Status"),
                        rs.getString("Message"),
                        toStringOrNull(rs.getTimestamp("StartedAt")),
                        toStringOrNull(rs.getTimestamp("FinishedAt"))
                );
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to get load session status: " + loadSessionId, e);
        }
    }

    private String toStringOrNull(Timestamp timestamp) {
        return timestamp != null
                ? timestamp.toLocalDateTime().toString()
                : null;
    }
}