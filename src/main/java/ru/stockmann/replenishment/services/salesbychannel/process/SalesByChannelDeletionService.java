package ru.stockmann.replenishment.services.salesbychannel.process;

import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDataDeleteResult;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDeletionSession;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDeletionSessionRepository;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class SalesByChannelDeletionService {

    private final DataSource dataSource;
    private final SalesByChannelDeletionRepository repository;
    private final DWHDeletionSessionRepository sessionRepository;

    public SalesByChannelDeletionService(
            DataSource dataSource,
            SalesByChannelDeletionRepository repository
    ) {
        this(dataSource, repository, new DWHDeletionSessionRepository(dataSource));
    }

    public SalesByChannelDeletionService(
            DataSource dataSource,
            SalesByChannelDeletionRepository repository,
            DWHDeletionSessionRepository sessionRepository
    ) {
        this.dataSource = dataSource;
        this.repository = repository;
        this.sessionRepository = sessionRepository;
    }

    public DWHDataDeleteResult deleteByLoadSessionId(long loadSessionId) {
        if (loadSessionId <= 0) {
            throw new IllegalArgumentException("loadSessionId must be positive");
        }
        return delete(
                DWHDeletionSession.byLoadSession(
                        DWHExcelLoadType.SALES_BY_CHANNEL, loadSessionId
                ),
                connection -> repository.deleteByLoadSessionId(connection, loadSessionId)
        );
    }

    public DWHDataDeleteResult deleteByYearAndMonth(String year, String month) {
        requireText(year, "year");
        requireText(month, "month");
        requireMaxLength(year, "year", 50);
        requireMaxLength(month, "month", 50);
        return delete(
                DWHDeletionSession.byTextYearAndMonth(
                        DWHExcelLoadType.SALES_BY_CHANNEL,
                        year,
                        month
                ),
                connection -> repository.deleteByYearAndMonth(connection, year, month)
        );
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private void requireMaxLength(String value, String name, int maxLength) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(name + " must not be longer than " + maxLength + " characters");
        }
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
                        : new RuntimeException(
                                "Failed to delete SalesByChannel target rows", e
                        );
                completeError(deletionSessionId, failure);
                throw failure;
            } finally {
                restoreAutoCommitQuietly(connection, oldAutoCommit);
            }
        } catch (SQLException e) {
            RuntimeException failure = new RuntimeException(
                    "Failed to open SalesByChannel delete transaction", e
            );
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
