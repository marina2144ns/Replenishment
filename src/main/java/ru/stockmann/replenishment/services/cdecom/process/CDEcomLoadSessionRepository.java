package ru.stockmann.replenishment.services.cdecom.process;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CDEcomLoadSessionRepository {

    private static final String LOAD_TYPE_CODE = "CD_ECOM";

    private final DataSource dataSource;

    public CDEcomLoadSessionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean existsById(long loadSessionId) {
        try (Connection connection = dataSource.getConnection()) {
            return existsById(connection, loadSessionId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check CDEcom load session. loadSessionId=" + loadSessionId, e);
        }
    }

    public boolean existsById(Connection connection, long loadSessionId) {
        String sql = """
                SELECT 1
                FROM dbo.DWH_Excel_Load_Session
                WHERE Id = ?
                  AND LoadTypeCode = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            ps.setString(2, LOAD_TYPE_CODE);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check CDEcom load session. loadSessionId=" + loadSessionId, e);
        }
    }
}
