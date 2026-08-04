package ru.stockmann.replenishment.services.cdecom.process;

import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDataDeleteResult;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDeletionSession;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDeletionSessionRepository;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class CDEcomDeletionService {

    private final DataSource dataSource;
    private final CDEcomDeletionRepository repository;
    private final DWHDeletionSessionRepository sessionRepository;

    public CDEcomDeletionService(DataSource dataSource, CDEcomDeletionRepository repository) {
        this(dataSource, repository, new DWHDeletionSessionRepository(dataSource));
    }

    public CDEcomDeletionService(
            DataSource dataSource,
            CDEcomDeletionRepository repository,
            DWHDeletionSessionRepository sessionRepository
    ) {
        this.dataSource = dataSource;
        this.repository = repository;
        this.sessionRepository = sessionRepository;
    }

    public DWHDataDeleteResult deleteByPeriod(int year, int week) {
        return delete(
                DWHDeletionSession.byPeriod(DWHExcelLoadType.CD_ECOM, year, week),
                connection -> repository.deleteByPeriod(connection, year, week)
        );
    }

    public DWHDataDeleteResult deleteByLoadSessionId(long loadSessionId) {
        if (loadSessionId <= 0) {
            throw new IllegalArgumentException("loadSessionId must be positive");
        }
        return delete(
                DWHDeletionSession.byLoadSession(DWHExcelLoadType.CD_ECOM, loadSessionId),
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
                        : new RuntimeException("Failed to delete CDEcom target rows", e);
                completeError(deletionSessionId, failure);
                throw failure;
            } finally {
                restoreAutoCommitQuietly(connection, oldAutoCommit);
            }
        } catch (SQLException e) {
            RuntimeException failure =
                    new RuntimeException("Failed to open CDEcom delete transaction", e);
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
