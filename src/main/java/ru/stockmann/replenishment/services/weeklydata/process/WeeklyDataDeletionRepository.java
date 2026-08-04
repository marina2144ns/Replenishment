package ru.stockmann.replenishment.services.weeklydata.process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class WeeklyDataDeletionRepository {

    public int deleteByPeriod(Connection connection, short year, short week) {
        String sql = """
                DELETE FROM dbo.Weekly_data
                WHERE [Year] = ?
                  AND [Week] = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setShort(1, year);
            ps.setShort(2, week);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete Weekly_data rows by period", e);
        }
    }

    public int deleteByLoadSessionId(Connection connection, long loadSessionId) {
        String sql = """
                DELETE FROM dbo.Weekly_data
                WHERE LoadSessionId = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete Weekly_data rows by load session", e);
        }
    }
}
