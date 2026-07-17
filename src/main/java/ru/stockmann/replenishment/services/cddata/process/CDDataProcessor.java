package ru.stockmann.replenishment.services.cddata.process;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CDDataProcessor {

    private final DataSource dataSource;
    private final CDDataLoadSessionRepository loadSessionRepository;
    private final CDDataRawRepository rawRepository;
    private final CDDataTargetRepository targetRepository;
    private final CDDataErrorRepository errorRepository;
    private final CDDataValidator validator;
    private final CDDataRowMapper mapper;

    public CDDataProcessor(
            DataSource dataSource,
            CDDataLoadSessionRepository loadSessionRepository,
            CDDataRawRepository rawRepository,
            CDDataTargetRepository targetRepository,
            CDDataErrorRepository errorRepository,
            CDDataValidator validator,
            CDDataRowMapper mapper
    ) {
        this.dataSource = dataSource;
        this.loadSessionRepository = loadSessionRepository;
        this.rawRepository = rawRepository;
        this.targetRepository = targetRepository;
        this.errorRepository = errorRepository;
        this.validator = validator;
        this.mapper = mapper;
    }

    public CDDataProcessResult process(long loadSessionId) {
        List<CDDataRawRow> rawRows = null;

        try {
            if (!loadSessionRepository.existsById(loadSessionId)) {
                return new CDDataProcessResult(
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

                    List<CDDataValidationError> errors = new ArrayList<>();

                    for (CDDataRawRow row : rawRows) {
                        CDDataValidationResult result = validator.validate(row);
                        if (!result.valid()) {
                            errors.addAll(result.errors());
                        }
                    }

                    if (!errors.isEmpty()) {
                        errorRepository.insertAll(connection, errors);
                        connection.commit();
                        return new CDDataProcessResult(
                                loadSessionId,
                                false,
                                rawRows.size(),
                                0,
                                errors.size(),
                                "Validation failed"
                        );
                    }

                    List<CDDataTargetRow> targetRows = new ArrayList<>();
                    for (CDDataRawRow row : rawRows) {
                        targetRows.add(mapper.toTargetRow(row));
                    }

                    if (!targetRows.isEmpty()) {
                        targetRepository.insertAll(connection, targetRows);
                    }
                    connection.commit();

                    return new CDDataProcessResult(
                            loadSessionId,
                            true,
                            rawRows.size(),
                            targetRows.size(),
                            0,
                            "CDData load session processed successfully"
                    );
                } catch (RuntimeException e) {
                    rollbackQuietly(connection);
                    throw e;
                } catch (SQLException e) {
                    rollbackQuietly(connection);
                    throw new RuntimeException(
                            "Failed to process CDData load session. loadSessionId=" + loadSessionId,
                            e
                    );
                } finally {
                    restoreAutoCommitQuietly(connection, oldAutoCommit);
                }
            } catch (SQLException e) {
                throw new RuntimeException(
                        "Failed to open CDData transaction. loadSessionId=" + loadSessionId,
                        e
                );
            }
        } catch (RuntimeException e) {
            return handleUnexpectedProcessingError(loadSessionId, rawRows, e);
        }
    }

    private CDDataProcessResult handleUnexpectedProcessingError(
            long loadSessionId,
            List<CDDataRawRow> rawRows,
            RuntimeException exception
    ) {
        insertUnexpectedProcessingError(loadSessionId, exception);

        return new CDDataProcessResult(
                loadSessionId,
                false,
                rawRows == null ? 0 : rawRows.size(),
                0,
                1,
                exception.getMessage()
        );
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
            errorRepository.insertAll(List.of(new CDDataValidationError(
                    loadSessionId,
                    0L,
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
