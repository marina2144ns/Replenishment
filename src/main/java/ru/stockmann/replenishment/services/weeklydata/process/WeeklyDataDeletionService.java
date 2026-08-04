package ru.stockmann.replenishment.services.weeklydata.process;

import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDataDeleteResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class WeeklyDataDeletionService {

    private final DataSource dataSource;
    private final WeeklyDataDeletionRepository repository;

    public WeeklyDataDeletionService(DataSource dataSource, WeeklyDataDeletionRepository repository) {
        this.dataSource = dataSource;
        this.repository = repository;
    }

    public DWHDataDeleteResult deleteByPeriod(short year, short week) {
        return delete(connection -> repository.deleteByPeriod(connection, year, week));
    }

    public DWHDataDeleteResult deleteByLoadSessionId(long loadSessionId) {
        if (loadSessionId <= 0) {
            throw new IllegalArgumentException("loadSessionId must be positive");
        }
        return delete(connection -> repository.deleteByLoadSessionId(connection, loadSessionId));
    }

    private DWHDataDeleteResult delete(DeleteOperation operation) {
        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                int deletedRows = operation.execute(connection);
                connection.commit();
                return new DWHDataDeleteResult(deletedRows);
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                throw e instanceof RuntimeException runtimeException
                        ? runtimeException
                        : new RuntimeException("Failed to delete WeeklyData target rows", e);
            } finally {
                restoreAutoCommitQuietly(connection, oldAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open WeeklyData delete transaction", e);
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    private void restoreAutoCommitQuietly(Connection connection, boolean oldAutoCommit) {
        try {
            connection.setAutoCommit(oldAutoCommit);
        } catch (SQLException ignored) {
        }
    }

    @FunctionalInterface
    private interface DeleteOperation {
        int execute(Connection connection) throws SQLException;
    }
}
