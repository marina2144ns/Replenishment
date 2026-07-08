package ru.stockmann.replenishment.services.weeklydata.process;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WeeklyDataProcessor {

    private final DataSource dataSource;
    private final WeeklyDataLoadSessionRepository loadSessionRepository;
    private final WeeklyDataRawRepository rawRepository;
    private final WeeklyDataErrorRepository errorRepository;
    private final WeeklyDataTargetRepository targetRepository;
    private final WeeklyDataValidator validator;
    private final WeeklyDataRowMapper mapper;

    public WeeklyDataProcessor(
            DataSource dataSource,
            WeeklyDataLoadSessionRepository loadSessionRepository,
            WeeklyDataRawRepository rawRepository,
            WeeklyDataErrorRepository errorRepository,
            WeeklyDataTargetRepository targetRepository,
            WeeklyDataValidator validator,
            WeeklyDataRowMapper mapper
    ) {
        this.dataSource = dataSource;
        this.loadSessionRepository = loadSessionRepository;
        this.rawRepository = rawRepository;
        this.errorRepository = errorRepository;
        this.targetRepository = targetRepository;
        this.validator = validator;
        this.mapper = mapper;
    }

    public WeeklyDataProcessResult process(long loadSessionId) {
        List<WeeklyDataRawRow> rawRows = null;

        try {
            if (!loadSessionRepository.existsById(loadSessionId)) {
                return new WeeklyDataProcessResult(
                        loadSessionId,
                        false,
                        0,
                        0,
                        1,
                        "Load session not found: " + loadSessionId
                );
            }

            try (Connection connection = dataSource.getConnection()) {
                boolean oldAutoCommit = connection.getAutoCommit();

                try {
                    connection.setAutoCommit(false);

                    rawRows = rawRepository.findByLoadSessionId(connection, loadSessionId);

                    errorRepository.deleteByLoadSessionId(connection, loadSessionId);
                    targetRepository.deleteByLoadSessionId(connection, loadSessionId);

                    List<WeeklyDataValidationError> errors = new ArrayList<>();
                    for (WeeklyDataRawRow row : rawRows) {
                        errors.addAll(validator.validate(row));
                    }

                    if (!errors.isEmpty()) {
                        errorRepository.insertAll(connection, errors);
                        connection.commit();
                        return new WeeklyDataProcessResult(
                                loadSessionId,
                                false,
                                rawRows.size(),
                                0,
                                errors.size(),
                                "Validation failed"
                        );
                    }

                    List<WeeklyDataTargetRow> targetRows = new ArrayList<>();
                    for (WeeklyDataRawRow row : rawRows) {
                        targetRows.add(mapper.toTargetRow(row));
                    }

                    targetRepository.insertAll(connection, targetRows);
                    connection.commit();

                    return new WeeklyDataProcessResult(
                            loadSessionId,
                            true,
                            rawRows.size(),
                            targetRows.size(),
                            0,
                            "WeeklyData load session processed successfully"
                    );
                } catch (RuntimeException e) {
                    rollbackQuietly(connection);
                    throw e;
                } catch (SQLException e) {
                    rollbackQuietly(connection);
                    throw new RuntimeException(
                            "Failed to process WeeklyData load session. loadSessionId=" + loadSessionId,
                            e
                    );
                } finally {
                    restoreAutoCommitQuietly(connection, oldAutoCommit);
                }
            } catch (SQLException e) {
                throw new RuntimeException(
                        "Failed to open WeeklyData transaction. loadSessionId=" + loadSessionId,
                        e
                );
            }
        } catch (RuntimeException e) {
            insertUnexpectedProcessingError(loadSessionId, e);

            return new WeeklyDataProcessResult(
                    loadSessionId,
                    false,
                    rawRows == null ? 0 : rawRows.size(),
                    0,
                    1,
                    e.getMessage()
            );
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

    private void insertUnexpectedProcessingError(long loadSessionId, RuntimeException exception) {
        String message = exception.getMessage();

        try {
            errorRepository.insertAll(List.of(new WeeklyDataValidationError(
                    loadSessionId,
                    0,
                    null,
                    "PROCESSING",
                    null,
                    "UNEXPECTED_PROCESSING_ERROR",
                    message,
                    "Unexpected processing error: " + message
            )));
        } catch (RuntimeException ignored) {
        }
    }
}
