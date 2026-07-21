package ru.stockmann.replenishment.services.cdecom.process;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CDEcomProcessor {

    private final DataSource dataSource;
    private final CDEcomLoadSessionRepository loadSessionRepository;
    private final CDEcomRawRepository rawRepository;
    private final CDEcomTargetRepository targetRepository;
    private final CDEcomErrorRepository errorRepository;
    private final CDEcomValidator validator;
    private final CDEcomRowMapper mapper;

    public CDEcomProcessor(
            DataSource dataSource,
            CDEcomLoadSessionRepository loadSessionRepository,
            CDEcomRawRepository rawRepository,
            CDEcomTargetRepository targetRepository,
            CDEcomErrorRepository errorRepository,
            CDEcomValidator validator,
            CDEcomRowMapper mapper
    ) {
        this.dataSource = dataSource;
        this.loadSessionRepository = loadSessionRepository;
        this.rawRepository = rawRepository;
        this.targetRepository = targetRepository;
        this.errorRepository = errorRepository;
        this.validator = validator;
        this.mapper = mapper;
    }

    public CDEcomProcessResult process(long loadSessionId) {
        List<CDEcomRawRow> rawRows = null;

        try {
            if (!loadSessionRepository.existsById(loadSessionId)) {
                return new CDEcomProcessResult(
                        loadSessionId,
                        false,
                        0,
                        0,
                        1,
                        "Load session not found or has unexpected LoadTypeCode. loadSessionId="
                                + loadSessionId + ", expected LoadTypeCode=CD_ECOM"
                );
            }

            try (Connection connection = dataSource.getConnection()) {
                boolean oldAutoCommit = connection.getAutoCommit();

                try {
                    connection.setAutoCommit(false);

                    rawRows = rawRepository.findByLoadSessionId(connection, loadSessionId);
                    errorRepository.deleteByLoadSessionId(connection, loadSessionId);
                    targetRepository.deleteByLoadSessionId(connection, loadSessionId);
                    List<CDEcomValidationError> errors = new ArrayList<>();
                    for (CDEcomRawRow row : rawRows) {
                        CDEcomValidationResult result = validator.validate(row);
                        if (!result.valid()) {
                            errors.addAll(result.errors());
                        }
                    }

                    if (!errors.isEmpty()) {
                        errorRepository.insertAll(connection, errors);
                        connection.commit();
                        return new CDEcomProcessResult(
                                loadSessionId,
                                false,
                                rawRows.size(),
                                0,
                                errors.size(),
                                "Validation failed"
                        );
                    }

                    List<CDEcomTargetRow> targetRows = new ArrayList<>();
                    for (CDEcomRawRow row : rawRows) {
                        targetRows.add(mapper.toTargetRow(row));
                    }

                    if (!targetRows.isEmpty()) {
                        targetRepository.insertAll(connection, targetRows);
                    }

                    connection.commit();
                    return new CDEcomProcessResult(
                            loadSessionId,
                            true,
                            rawRows.size(),
                            targetRows.size(),
                            0,
                            "CDEcom load session processed successfully"
                    );
                } catch (RuntimeException e) {
                    rollbackQuietly(connection);
                    throw e;
                } catch (SQLException e) {
                    rollbackQuietly(connection);
                    throw new RuntimeException("Failed to process CDEcom load session. loadSessionId=" + loadSessionId, e);
                } finally {
                    restoreAutoCommitQuietly(connection, oldAutoCommit);
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to open CDEcom transaction. loadSessionId=" + loadSessionId, e);
            }
        } catch (RuntimeException e) {
            return handleUnexpectedProcessingError(loadSessionId, rawRows, e);
        }
    }

    private CDEcomProcessResult handleUnexpectedProcessingError(
            long loadSessionId,
            List<CDEcomRawRow> rawRows,
            RuntimeException exception
    ) {
        insertUnexpectedProcessingError(loadSessionId, exception);

        return new CDEcomProcessResult(
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
            errorRepository.insertAll(List.of(new CDEcomValidationError(
                    loadSessionId,
                    0L,
                    null,
                    "PROCESSING",
                    null,
                    "UNEXPECTED_PROCESSING_ERROR",
                    trim(message, 500),
                    trim("Unexpected processing error: " + message, 4000)
            )));
        } catch (RuntimeException ignored) {
        }
    }

    private static String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
