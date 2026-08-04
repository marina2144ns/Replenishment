package ru.stockmann.replenishment.services.salesbychannel.process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SalesByChannelDeletionRepository {

    public int deleteByPeriod(Connection connection, int year, int week) {
        String sql = """
                DELETE FROM dbo.SalesByChannel
                WHERE [year] = ?
                  AND [month] = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, Integer.toString(year));
            ps.setString(2, Integer.toString(week));
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete SalesByChannel rows by period", e);
        }
    }

    public int deleteByLoadSessionId(Connection connection, long loadSessionId) {
        String sql = """
                DELETE FROM dbo.SalesByChannel
                WHERE LoadSessionId = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to delete SalesByChannel rows by load session", e
            );
        }
    }
}
