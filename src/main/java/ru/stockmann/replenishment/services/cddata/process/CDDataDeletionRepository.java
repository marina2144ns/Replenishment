package ru.stockmann.replenishment.services.cddata.process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CDDataDeletionRepository {

    public int deleteByPeriod(Connection connection, int year, int week) {
        String sql = """
                DELETE FROM dbo.CD_data
                WHERE god = ?
                  AND sezon = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, week);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete CD_data rows by period", e);
        }
    }

    public int deleteByLoadSessionId(Connection connection, long loadSessionId) {
        String sql = """
                DELETE FROM dbo.CD_data
                WHERE LoadSessionId = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete CD_data rows by load session", e);
        }
    }
}
