package ru.stockmann.replenishment.services.weeklydata.process;

import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDataDeleteResult;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDeletionSession;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDeletionSessionRepository;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class WeeklyDataDeletionService {

    private final DataSource dataSource;
    private final WeeklyDataDeletionRepository repository;
    private final DWHDeletionSessionRepository sessionRepository;

    public WeeklyDataDeletionService(DataSource dataSource, WeeklyDataDeletionRepository repository) {
        this(dataSource, repository, new DWHDeletionSessionRepository(dataSource));
    }

    public WeeklyDataDeletionService(
            DataSource dataSource,
            WeeklyDataDeletionRepository repository,
            DWHDeletionSessionRepository sessionRepository
    ) {
        this.dataSource = dataSource;
        this.repository = repository;
        this.sessionRepository = sessionRepository;
    }

    public DWHDataDeleteResult deleteByPeriod(short year, short week) {
        return delete(
                DWHDeletionSession.byPeriod(DWHExcelLoadType.WEEKLY_DATA, year, week),
                connection -> repository.deleteByPeriod(connection, year, week)
        );
    }

    public DWHDataDeleteResult deleteByLoadSessionId(long loadSessionId) {
        if (loadSessionId <= 0) {
            throw new IllegalArgumentException("loadSessionId must be positive");
        }
        return delete(
                DWHDeletionSession.byLoadSession(DWHExcelLoadType.WEEKLY_DATA, loadSessionId),
                connection -> repository.deleteByLoadSessionId(connection, loadSessionId)
        );
    }

    private DWHDataDeleteResult delete(
            DWHDeletionSession session,
            DeleteOperation operation
    ) {
        long deletionSessionId = sessionRepository.create(session);
        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                int deletedRows = operation.execute(connection);
                sessionRepository.completeSuccess(connection, deletionSessionId, deletedRows);
                connection.commit();
                return new DWHDataDeleteResult(deletedRows);
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                RuntimeException failure = e instanceof RuntimeException runtimeException
                        ? runtimeException
                        : new RuntimeException("Failed to delete WeeklyData target rows", e);
                completeError(deletionSessionId, failure);
                throw failure;
            } finally {
                restoreAutoCommitQuietly(connection, oldAutoCommit);
            }
        } catch (SQLException e) {
            RuntimeException failure =
                    new RuntimeException("Failed to open WeeklyData delete transaction", e);
            completeError(deletionSessionId, failure);
            throw failure;
        }
    }

    private void completeError(long deletionSessionId, RuntimeException failure) {
        try {
            sessionRepository.completeError(deletionSessionId, failure.getMessage());
        } catch (RuntimeException loggingFailure) {
            failure.addSuppressed(loggingFailure);
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
