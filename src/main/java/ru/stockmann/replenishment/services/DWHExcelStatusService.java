package ru.stockmann.replenishment.services;

import org.springframework.stereotype.Service;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Service
public class DWHExcelStatusService {

    private final DataSource dataSource;

    public DWHExcelStatusService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DWHExcelLoadResult getStatus(Long loadSessionId) {

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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, loadSessionId);

            try (ResultSet rs = stmt.executeQuery()) {

                if (!rs.next()) {
                    return DWHExcelLoadResult.error(loadSessionId, "LoadSession not found");
                }

                return new DWHExcelLoadResult(
                        rs.getLong("Id"),
                        rs.getString("Status"),
                        rs.getString("Message")
                );
            }

        } catch (Exception ex) {
            throw new RuntimeException("Error reading load status", ex);
        }
    }
}