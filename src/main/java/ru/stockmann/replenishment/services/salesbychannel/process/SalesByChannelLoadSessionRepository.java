package ru.stockmann.replenishment.services.salesbychannel.process;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SalesByChannelLoadSessionRepository {

    private static final String LOAD_TYPE_CODE = "SALES_BY_CHANNEL";
    private final DataSource dataSource;

    public SalesByChannelLoadSessionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean existsById(long loadSessionId) {
        String sql = """
                SELECT 1
                FROM dbo.DWH_Excel_Load_Session
                WHERE Id = ?
                  AND LoadTypeCode = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            ps.setString(2, LOAD_TYPE_CODE);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to check SalesByChannel load session. loadSessionId=" + loadSessionId, e
            );
        }
    }
}
