package ru.stockmann.replenishment.services.cdecom.process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CDEcomDeletionRepository {

    public int deleteByPeriod(Connection connection, int year, int week) {
        String sql = """
                DELETE FROM dbo.CD_ecom
                WHERE [year] = ?
                  AND season = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, week);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete CD_ecom rows by period", e);
        }
    }

    public int deleteByLoadSessionId(Connection connection, long loadSessionId) {
        String sql = """
                DELETE FROM dbo.CD_ecom
                WHERE LoadSessionId = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete CD_ecom rows by load session", e);
        }
    }

    public int deleteByNazvanieAndDen(Connection connection, String nazvanie, int den) {
        String sql = """
                DELETE FROM dbo.CD_ecom
                WHERE name = ?
                  AND [day] = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nazvanie);
            ps.setInt(2, den);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete CD_ecom rows by name and day", e);
        }
    }
}
