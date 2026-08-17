package ru.stockmann.replenishment.services.salesbychannel.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SalesByChannelProcessor {

    private static final Logger log = LoggerFactory.getLogger(SalesByChannelProcessor.class);
    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final DataSource dataSource;
    private final SalesByChannelLoadSessionRepository loadSessionRepository;
    private final SalesByChannelRawRepository rawRepository;
    private final SalesByChannelStageRepository stageRepository;
    private final SalesByChannelErrorRepository errorRepository;
    private final SalesByChannelTargetRepository targetRepository;
    private final SalesByChannelValidator validator;
    private final int chunkSize;

    public SalesByChannelProcessor(
            DataSource dataSource,
            SalesByChannelLoadSessionRepository loadSessionRepository,
            SalesByChannelRawRepository rawRepository,
            SalesByChannelStageRepository stageRepository,
            SalesByChannelErrorRepository errorRepository,
            SalesByChannelTargetRepository targetRepository,
            SalesByChannelValidator validator,
            int chunkSize
    ) {
        this.dataSource = dataSource;
        this.loadSessionRepository = loadSessionRepository;
        this.rawRepository = rawRepository;
        this.stageRepository = stageRepository;
        this.errorRepository = errorRepository;
        this.targetRepository = targetRepository;
        this.validator = validator;
        this.chunkSize = chunkSize;
    }

    public SalesByChannelProcessResult process(long loadSessionId) {
        long startedAt = System.nanoTime();
        long totalRows = 0;
        long stagedRows = 0;
        long loadedRows = 0;
        long errorRows = 0;
        boolean sessionValidated = false;

        log.info("SalesByChannel processing started. loadSessionId={}, chunkSize={}", loadSessionId, chunkSize);
        try {
            if (!loadSessionRepository.existsById(loadSessionId)) {
                return result(loadSessionId, false, 0, 0, 0, 0,
                        "Load session not found or has unexpected LoadTypeCode. loadSessionId="
                                + loadSessionId + ", expected LoadTypeCode=SALES_BY_CHANNEL");
            }
            sessionValidated = true;
            cleanup(loadSessionId);

            long lastRawId = SalesByChannelRawRepository.INITIAL_LAST_RAW_ID;
            int chunkNumber = 0;
            while (true) {
                List<SalesByChannelRawRow> rawChunk =
                        rawRepository.findChunk(loadSessionId, lastRawId);
                if (rawChunk.isEmpty()) {
                    break;
                }

                chunkNumber++;
                long chunkStartedAt = System.nanoTime();
                List<SalesByChannelStageRow> stageChunk = new ArrayList<>(rawChunk.size());
                List<SalesByChannelValidationError> errorChunk = new ArrayList<>();
                for (SalesByChannelRawRow rawRow : rawChunk) {
                    SalesByChannelRowValidationResult validation = validator.validateAndMap(rawRow);
                    if (validation.valid()) {
                        stageChunk.add(validation.stageRow());
                    } else {
                        errorChunk.addAll(validation.errors());
                    }
                }

                writeChunk(loadSessionId, chunkNumber, stageChunk, errorChunk);

                lastRawId = rawChunk.stream()
                        .mapToLong(SalesByChannelRawRow::id)
                        .max()
                        .orElseThrow();
                totalRows += rawChunk.size();
                stagedRows += stageChunk.size();
                errorRows += errorChunk.size();
                log.info("SalesByChannel chunk completed. loadSessionId={}, chunkNumber={}, lastRawId={}, "
                                + "chunkRows={}, cumulativeTotalRows={}, cumulativeStagedRows={}, "
                                + "cumulativeErrorRows={}, elapsedMs={}, usedMemoryMb={}",
                        loadSessionId, chunkNumber, lastRawId, rawChunk.size(), totalRows,
                        stagedRows, errorRows, elapsedMs(chunkStartedAt), usedMemoryMb());
            }

            if (errorRows > 0) {
                return result(loadSessionId, false, totalRows, stagedRows, 0, errorRows,
                        "Validation failed; target was not changed");
            }
            if (stagedRows != totalRows) {
                throw new IllegalStateException(
                        "SalesByChannel processing counter mismatch. loadSessionId=" + loadSessionId
                                + ", totalRows=" + totalRows + ", stagedRows=" + stagedRows
                );
            }

            loadedRows = publish(loadSessionId, stagedRows);
            return result(loadSessionId, true, totalRows, stagedRows, loadedRows, 0,
                    "SalesByChannel processed and published successfully");
        } catch (RuntimeException e) {
            errorRows++;
            if (sessionValidated) {
                errorRepository.insertBestEffort(new SalesByChannelValidationError(
                        loadSessionId, 0L, null, "PROCESSING", null,
                        "UNEXPECTED_PROCESSING_ERROR", trim(e.getMessage(), 500),
                        trim("Unexpected processing error: " + e.getMessage(), 4000)
                ));
            }
            return result(loadSessionId, false, totalRows, stagedRows, loadedRows,
                    errorRows, e.getMessage());
        } finally {
            log.info("SalesByChannel processing finished. loadSessionId={}, elapsedMs={}, totalRows={}, "
                            + "stagedRows={}, loadedRows={}, errorRows={}",
                    loadSessionId, elapsedMs(startedAt), totalRows, stagedRows, loadedRows, errorRows);
            logMemory(loadSessionId);
        }
    }

    private void cleanup(long loadSessionId) {
        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                stageRepository.deleteByLoadSessionId(connection, loadSessionId);
                errorRepository.deleteByLoadSessionId(connection, loadSessionId);
                connection.commit();
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                throw asRuntime("Failed to clean SalesByChannel staging", loadSessionId, e);
            } finally {
                restoreAutoCommit(connection, oldAutoCommit);
            }
        } catch (SQLException e) {
            throw asRuntime("Failed to open SalesByChannel cleanup transaction", loadSessionId, e);
        }
    }

    private void writeChunk(
            long loadSessionId,
            int chunkNumber,
            List<SalesByChannelStageRow> stageRows,
            List<SalesByChannelValidationError> errors
    ) {
        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                stageRepository.insertBatch(connection, loadSessionId, stageRows);
                errorRepository.insertBatch(connection, loadSessionId, errors);
                connection.commit();
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                throw asRuntime(
                        "Failed to write SalesByChannel chunk " + chunkNumber, loadSessionId, e
                );
            } finally {
                restoreAutoCommit(connection, oldAutoCommit);
            }
        } catch (SQLException e) {
            throw asRuntime("Failed to open SalesByChannel chunk transaction", loadSessionId, e);
        }
    }

    private long publish(long loadSessionId, long expectedRows) {
        long startedAt = System.nanoTime();
        log.info("SalesByChannel publish started. loadSessionId={}, expectedRows={}",
                loadSessionId, expectedRows);
        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                int publishedRows = targetRepository.publishFromStage(connection, loadSessionId);
                if (publishedRows != expectedRows) {
                    throw new IllegalStateException(
                            "SalesByChannel publish row count mismatch. loadSessionId=" + loadSessionId
                                    + ", expectedRows=" + expectedRows
                                    + ", publishedRows=" + publishedRows
                    );
                }

                int cleanedRows = stageRepository.deleteByLoadSessionId(connection, loadSessionId);
                if (cleanedRows != expectedRows) {
                    throw new IllegalStateException(
                            "SalesByChannel stage cleanup row count mismatch. loadSessionId=" + loadSessionId
                                    + ", expectedRows=" + expectedRows
                                    + ", cleanedRows=" + cleanedRows
                    );
                }

                connection.commit();
                log.info("SalesByChannel publish commit completed. loadSessionId={}, publishedRows={}, "
                                + "elapsedMs={}",
                        loadSessionId, publishedRows, elapsedMs(startedAt));
                return publishedRows;
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                log.info("SalesByChannel publish rollback. loadSessionId={}, reason={}",
                        loadSessionId, e.getMessage());
                throw asRuntime("Failed to publish SalesByChannel", loadSessionId, e);
            } finally {
                restoreAutoCommit(connection, oldAutoCommit);
            }
        } catch (SQLException e) {
            throw asRuntime("Failed to open SalesByChannel publish transaction", loadSessionId, e);
        }
    }

    private SalesByChannelProcessResult result(
            long loadSessionId,
            boolean success,
            long totalRows,
            long stagedRows,
            long loadedRows,
            long errorRows,
            String message
    ) {
        return new SalesByChannelProcessResult(
                loadSessionId, success, totalRows, stagedRows, loadedRows, errorRows, message
        );
    }

    private RuntimeException asRuntime(String message, long loadSessionId, Exception cause) {
        if (cause instanceof RuntimeException runtime) return runtime;
        return new RuntimeException(message + ". loadSessionId=" + loadSessionId, cause);
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    private void restoreAutoCommit(Connection connection, boolean oldAutoCommit) {
        try {
            connection.setAutoCommit(oldAutoCommit);
        } catch (SQLException ignored) {
        }
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private long usedMemoryMb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MB;
    }

    private void logMemory(long loadSessionId) {
        Runtime runtime = Runtime.getRuntime();
        log.info("SalesByChannel memory. loadSessionId={}, usedMemoryMb={}, totalMemoryMb={}, "
                        + "freeMemoryMb={}, maxMemoryMb={}",
                loadSessionId, usedMemoryMb(), runtime.totalMemory() / BYTES_PER_MB,
                runtime.freeMemory() / BYTES_PER_MB, runtime.maxMemory() / BYTES_PER_MB);
    }

    private static String trim(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
