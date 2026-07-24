package ru.stockmann.replenishment.services.weeklydata.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WeeklyDataProcessor {

    private static final Logger log = LoggerFactory.getLogger(WeeklyDataProcessor.class);
    private static final int RAW_CHUNK_SIZE = WeeklyDataRawRepository.DEFAULT_CHUNK_SIZE;
    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final DataSource dataSource;
    private final WeeklyDataLoadSessionRepository loadSessionRepository;
    private final WeeklyDataRawRepository rawRepository;
    private final WeeklyDataErrorRepository errorRepository;
    private final WeeklyDataStageRepository stageRepository;
    private final WeeklyDataTargetRepository targetRepository;
    private final WeeklyDataValidator validator;

    public WeeklyDataProcessor(
            DataSource dataSource,
            WeeklyDataLoadSessionRepository loadSessionRepository,
            WeeklyDataRawRepository rawRepository,
            WeeklyDataErrorRepository errorRepository,
            WeeklyDataStageRepository stageRepository,
            WeeklyDataTargetRepository targetRepository,
            WeeklyDataValidator validator
    ) {
        this.dataSource = dataSource;
        this.loadSessionRepository = loadSessionRepository;
        this.rawRepository = rawRepository;
        this.errorRepository = errorRepository;
        this.stageRepository = stageRepository;
        this.targetRepository = targetRepository;
        this.validator = validator;
    }

    public WeeklyDataProcessResult process(long loadSessionId) {
        long processorStartedAt = System.nanoTime();
        long totalRows = 0;
        long stagedRows = 0;
        long loadedRows = 0;
        long errorRows = 0;

        log.info("WeeklyData processing started. loadSessionId={}, chunkSize={}",
                loadSessionId, RAW_CHUNK_SIZE);
        try {
            long phaseStartedAt = System.nanoTime();
            log.info("WeeklyData load session check started. loadSessionId={}", loadSessionId);
            boolean loadSessionExists = loadSessionRepository.existsById(loadSessionId);
            log.info("WeeklyData load session check finished. loadSessionId={}, elapsedMs={}, exists={}",
                    loadSessionId, elapsedMs(phaseStartedAt), loadSessionExists);
            if (!loadSessionExists) {
                errorRows = 1;
                return new WeeklyDataProcessResult(
                        loadSessionId,
                        false,
                        0,
                        0,
                        0,
                        errorRows,
                        "Load session not found or has unexpected LoadTypeCode. loadSessionId="
                                + loadSessionId + ", expected LoadTypeCode=WEEKLY_DATA"
                );
            }

            cleanupPreviousProcessing(loadSessionId);
            logMemory("cleanup", loadSessionId);

            long lastRawId = WeeklyDataRawRepository.INITIAL_LAST_RAW_ID;
            int chunkNumber = 0;

            while (true) {
                phaseStartedAt = System.nanoTime();
                log.info("WeeklyData raw chunk read started. loadSessionId={}, chunkNumber={}, lastRawId={}, "
                                + "chunkSize={}",
                        loadSessionId, chunkNumber + 1, lastRawId, RAW_CHUNK_SIZE);
                List<WeeklyDataRawRow> rawChunk =
                        rawRepository.findChunk(loadSessionId, lastRawId);
                log.info("WeeklyData raw chunk read finished. loadSessionId={}, chunkNumber={}, elapsedMs={}, rows={}",
                        loadSessionId, chunkNumber + 1, elapsedMs(phaseStartedAt), rawChunk.size());

                if (rawChunk.isEmpty()) {
                    break;
                }

                chunkNumber++;
                long chunkStartedAt = System.nanoTime();
                List<WeeklyDataStageRow> stageRows = new ArrayList<>(rawChunk.size());
                List<WeeklyDataValidationError> errors = new ArrayList<>();

                phaseStartedAt = System.nanoTime();
                log.info("WeeklyData validation and typing started. loadSessionId={}, chunkNumber={}, rows={}",
                        loadSessionId, chunkNumber, rawChunk.size());
                for (WeeklyDataRawRow rawRow : rawChunk) {
                    WeeklyDataRowValidationResult result = validator.validateAndMap(rawRow);
                    if (result.valid()) {
                        stageRows.add(result.stageRow());
                    } else {
                        errors.addAll(result.errors());
                    }
                }
                log.info("WeeklyData validation and typing finished. loadSessionId={}, chunkNumber={}, "
                                + "elapsedMs={}, rows={}, stagedRows={}, errorRows={}",
                        loadSessionId, chunkNumber, elapsedMs(phaseStartedAt), rawChunk.size(),
                        stageRows.size(), errors.size());

                writeChunk(loadSessionId, chunkNumber, stageRows, errors);

                lastRawId = rawChunk.get(rawChunk.size() - 1).rawId();
                totalRows += rawChunk.size();
                stagedRows += stageRows.size();
                errorRows += errors.size();

                log.info("WeeklyData chunk completed. loadSessionId={}, chunkNumber={}, lastRawId={}, "
                                + "chunkRows={}, cumulativeTotalRows={}, cumulativeStagedRows={}, "
                                + "cumulativeErrorRows={}, elapsedMs={}, usedMemoryMb={}",
                        loadSessionId,
                        chunkNumber,
                        lastRawId,
                        rawChunk.size(),
                        totalRows,
                        stagedRows,
                        errorRows,
                        elapsedMs(chunkStartedAt),
                        usedMemoryMb());
            }

            log.info("WeeklyData processing counters. loadSessionId={}, totalRows={}, stagedRows={}, errorRows={}",
                    loadSessionId, totalRows, stagedRows, errorRows);

            if (errorRows > 0) {
                return new WeeklyDataProcessResult(
                        loadSessionId,
                        false,
                        totalRows,
                        stagedRows,
                        0,
                        errorRows,
                        "Validation failed; target was not changed"
                );
            }

            if (stagedRows != totalRows) {
                throw new IllegalStateException(
                        "WeeklyData processing counter mismatch. loadSessionId=" + loadSessionId
                                + ", totalRows=" + totalRows
                                + ", stagedRows=" + stagedRows
                );
            }

            loadedRows = publish(loadSessionId, stagedRows);
            return new WeeklyDataProcessResult(
                    loadSessionId,
                    true,
                    totalRows,
                    stagedRows,
                    loadedRows,
                    errorRows,
                    "WeeklyData load session processed and published successfully"
            );
        } catch (RuntimeException e) {
            insertUnexpectedProcessingError(loadSessionId, e);
            errorRows++;
            return new WeeklyDataProcessResult(
                    loadSessionId,
                    false,
                    totalRows,
                    stagedRows,
                    loadedRows,
                    errorRows,
                    e.getMessage()
            );
        } finally {
            log.info("WeeklyData processing finished. loadSessionId={}, elapsedMs={}, totalRows={}, "
                            + "stagedRows={}, loadedRows={}, errorRows={}",
                    loadSessionId, elapsedMs(processorStartedAt), totalRows, stagedRows, loadedRows, errorRows);
            logMemory("processor finish", loadSessionId);
        }
    }

    private long publish(long loadSessionId, long expectedRows) {
        long publishStartedAt = System.nanoTime();
        log.info("WeeklyData publish started. loadSessionId={}, expectedRows={}", loadSessionId, expectedRows);
        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                int publishedRows = targetRepository.publishFromStage(connection, loadSessionId);
                if (publishedRows != expectedRows) {
                    throw new IllegalStateException(
                            "WeeklyData publish row count mismatch. loadSessionId=" + loadSessionId
                                    + ", expectedRows=" + expectedRows
                                    + ", publishedRows=" + publishedRows
                    );
                }

                long cleanupStartedAt = System.nanoTime();
                int cleanedRows = stageRepository.deleteByLoadSessionId(connection, loadSessionId);
                if (cleanedRows != expectedRows) {
                    throw new IllegalStateException(
                            "WeeklyData stage cleanup row count mismatch. loadSessionId=" + loadSessionId
                                    + ", expectedRows=" + expectedRows
                                    + ", cleanedRows=" + cleanedRows
                    );
                }
                log.info("WeeklyData stage cleanup completed. loadSessionId={}, affectedRows={}, elapsedMs={}",
                        loadSessionId, cleanedRows, elapsedMs(cleanupStartedAt));

                long commitStartedAt = System.nanoTime();
                connection.commit();
                log.info("WeeklyData publish commit completed. loadSessionId={}, elapsedMs={}",
                        loadSessionId, elapsedMs(commitStartedAt));
                return publishedRows;
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                log.info("WeeklyData publish rollback. loadSessionId={}, reason={}",
                        loadSessionId, e.getMessage());
                throw processingFailure("Failed to publish WeeklyData", loadSessionId, e);
            } finally {
                restoreAutoCommitQuietly(connection, oldAutoCommit);
            }
        } catch (SQLException e) {
            throw processingFailure("Failed to open WeeklyData publish transaction", loadSessionId, e);
        } finally {
            log.info("WeeklyData publish finished. loadSessionId={}, elapsedMs={}",
                    loadSessionId, elapsedMs(publishStartedAt));
        }
    }

    private void cleanupPreviousProcessing(long loadSessionId) {
        long startedAt = System.nanoTime();
        log.info("WeeklyData stage/error cleanup started. loadSessionId={}", loadSessionId);
        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                stageRepository.deleteByLoadSessionId(connection, loadSessionId);
                errorRepository.deleteByLoadSessionId(connection, loadSessionId);
                connection.commit();
                log.info("WeeklyData stage/error cleanup finished. loadSessionId={}, elapsedMs={}",
                        loadSessionId, elapsedMs(startedAt));
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                throw processingFailure("Failed to clean WeeklyData staging", loadSessionId, e);
            } finally {
                restoreAutoCommitQuietly(connection, oldAutoCommit);
            }
        } catch (SQLException e) {
            throw processingFailure("Failed to open WeeklyData cleanup transaction", loadSessionId, e);
        }
    }

    private void writeChunk(
            long loadSessionId,
            int chunkNumber,
            List<WeeklyDataStageRow> stageRows,
            List<WeeklyDataValidationError> errors
    ) {
        long startedAt = System.nanoTime();
        log.info("WeeklyData chunk write started. loadSessionId={}, chunkNumber={}, stageRows={}, errorRows={}",
                loadSessionId, chunkNumber, stageRows.size(), errors.size());
        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                stageRepository.insertBatch(connection, loadSessionId, stageRows);
                errorRepository.insertBatch(connection, loadSessionId, errors);
                log.info("WeeklyData chunk commit started. loadSessionId={}, chunkNumber={}",
                        loadSessionId, chunkNumber);
                connection.commit();
                log.info("WeeklyData chunk commit finished. loadSessionId={}, chunkNumber={}",
                        loadSessionId, chunkNumber);
                log.info("WeeklyData chunk write finished. loadSessionId={}, chunkNumber={}, elapsedMs={}",
                        loadSessionId, chunkNumber, elapsedMs(startedAt));
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                throw processingFailure(
                        "Failed to write WeeklyData chunk " + chunkNumber,
                        loadSessionId,
                        e
                );
            } finally {
                restoreAutoCommitQuietly(connection, oldAutoCommit);
            }
        } catch (SQLException e) {
            throw processingFailure("Failed to open WeeklyData chunk transaction", loadSessionId, e);
        }
    }

    private RuntimeException processingFailure(String message, long loadSessionId, Exception cause) {
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new RuntimeException(message + ". loadSessionId=" + loadSessionId, cause);
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private long usedMemoryMb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MB;
    }

    private void logMemory(String phase, long loadSessionId) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;
        log.info("WeeklyData memory. loadSessionId={}, phase={}, usedMemoryMb={}, totalMemoryMb={}, "
                        + "freeMemoryMb={}, maxMemoryMb={}",
                loadSessionId,
                phase,
                usedMemory / BYTES_PER_MB,
                totalMemory / BYTES_PER_MB,
                freeMemory / BYTES_PER_MB,
                maxMemory / BYTES_PER_MB);
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
