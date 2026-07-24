package ru.stockmann.replenishment.services.cddata.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CDDataProcessor {

    private static final Logger log = LoggerFactory.getLogger(CDDataProcessor.class);
    private static final int RAW_CHUNK_SIZE = CDDataRawRepository.DEFAULT_CHUNK_SIZE;
    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final DataSource dataSource;
    private final CDDataLoadSessionRepository loadSessionRepository;
    private final CDDataRawRepository rawRepository;
    private final CDDataErrorRepository errorRepository;
    private final CDDataStageRepository stageRepository;
    private final CDDataTargetRepository targetRepository;
    private final CDDataValidator validator;

    public CDDataProcessor(
            DataSource dataSource,
            CDDataLoadSessionRepository loadSessionRepository,
            CDDataRawRepository rawRepository,
            CDDataErrorRepository errorRepository,
            CDDataStageRepository stageRepository,
            CDDataTargetRepository targetRepository,
            CDDataValidator validator
    ) {
        this.dataSource = dataSource;
        this.loadSessionRepository = loadSessionRepository;
        this.rawRepository = rawRepository;
        this.errorRepository = errorRepository;
        this.stageRepository = stageRepository;
        this.targetRepository = targetRepository;
        this.validator = validator;
    }

    public CDDataProcessResult process(long loadSessionId) {
        long processorStartedAt = System.nanoTime();
        long totalRows = 0;
        long stagedRows = 0;
        long loadedRows = 0;
        long errorRows = 0;

        log.info("CDData processing started. loadSessionId={}, chunkSize={}", loadSessionId, RAW_CHUNK_SIZE);
        try {
            long phaseStartedAt = System.nanoTime();
            log.info("CDData load session check started. loadSessionId={}", loadSessionId);
            boolean loadSessionExists = loadSessionRepository.existsById(loadSessionId);
            log.info("CDData load session check finished. loadSessionId={}, elapsedMs={}, exists={}",
                    loadSessionId, elapsedMs(phaseStartedAt), loadSessionExists);
            if (!loadSessionExists) {
                errorRows = 1;
                return result(loadSessionId, false, 0, 0, 0, errorRows,
                        "Load session not found or has unexpected LoadTypeCode. loadSessionId="
                                + loadSessionId + ", expected LoadTypeCode=CD_DATA");
            }

            cleanupPreviousProcessing(loadSessionId);
            logMemory("cleanup", loadSessionId);

            long lastRawId = CDDataRawRepository.INITIAL_LAST_RAW_ID;
            int chunkNumber = 0;
            while (true) {
                int nextChunkNumber = chunkNumber + 1;
                phaseStartedAt = System.nanoTime();
                log.info("CDData raw chunk read started. loadSessionId={}, chunkNumber={}, lastRawId={}, "
                                + "chunkSize={}",
                        loadSessionId, nextChunkNumber, lastRawId, RAW_CHUNK_SIZE);
                List<CDDataRawRow> rawChunk = rawRepository.findChunk(loadSessionId, lastRawId);
                log.info("CDData raw chunk read finished. loadSessionId={}, chunkNumber={}, elapsedMs={}, rows={}",
                        loadSessionId, nextChunkNumber, elapsedMs(phaseStartedAt), rawChunk.size());
                if (rawChunk.isEmpty()) {
                    break;
                }

                chunkNumber++;
                long chunkStartedAt = System.nanoTime();
                List<CDDataStageRow> stageRows = new ArrayList<>(rawChunk.size());
                List<CDDataValidationError> errors = new ArrayList<>();

                phaseStartedAt = System.nanoTime();
                log.info("CDData validation and typing started. loadSessionId={}, chunkNumber={}, rows={}",
                        loadSessionId, chunkNumber, rawChunk.size());
                for (CDDataRawRow rawRow : rawChunk) {
                    CDDataRowValidationResult validationResult = validator.validateAndMap(rawRow);
                    if (validationResult.valid()) {
                        stageRows.add(validationResult.stageRow());
                    } else {
                        errors.addAll(validationResult.errors());
                    }
                }
                log.info("CDData validation and typing finished. loadSessionId={}, chunkNumber={}, elapsedMs={}, "
                                + "rows={}, stagedRows={}, errorRows={}",
                        loadSessionId, chunkNumber, elapsedMs(phaseStartedAt), rawChunk.size(),
                        stageRows.size(), errors.size());

                writeChunk(loadSessionId, chunkNumber, stageRows, errors);

                lastRawId = rawChunk.get(rawChunk.size() - 1).id();
                totalRows += rawChunk.size();
                stagedRows += stageRows.size();
                errorRows += errors.size();
                log.info("CDData chunk completed. loadSessionId={}, chunkNumber={}, lastRawId={}, chunkRows={}, "
                                + "cumulativeTotalRows={}, cumulativeStagedRows={}, cumulativeErrorRows={}, "
                                + "elapsedMs={}, usedMemoryMb={}",
                        loadSessionId, chunkNumber, lastRawId, rawChunk.size(), totalRows, stagedRows,
                        errorRows, elapsedMs(chunkStartedAt), usedMemoryMb());
            }

            log.info("CDData processing counters. loadSessionId={}, totalRows={}, stagedRows={}, loadedRows=0, "
                            + "errorRows={}",
                    loadSessionId, totalRows, stagedRows, errorRows);
            if (errorRows > 0) {
                return result(loadSessionId, false, totalRows, stagedRows, 0, errorRows,
                        "Validation failed; target was not changed");
            }
            validatePublishCounters(loadSessionId, totalRows, stagedRows);

            loadedRows = publish(loadSessionId, stagedRows);
            return result(loadSessionId, true, totalRows, stagedRows, loadedRows, 0,
                    "CDData load session processed and published successfully");
        } catch (RuntimeException e) {
            insertUnexpectedProcessingError(loadSessionId, e);
            errorRows++;
            return result(loadSessionId, false, totalRows, stagedRows, loadedRows, errorRows, e.getMessage());
        } finally {
            log.info("CDData processing finished. loadSessionId={}, elapsedMs={}, totalRows={}, stagedRows={}, "
                            + "loadedRows={}, errorRows={}",
                    loadSessionId, elapsedMs(processorStartedAt), totalRows, stagedRows, loadedRows, errorRows);
            logMemory("processor finish", loadSessionId);
        }
    }

    private CDDataProcessResult result(
            long loadSessionId,
            boolean success,
            long totalRows,
            long stagedRows,
            long loadedRows,
            long errorRows,
            String message
    ) {
        return new CDDataProcessResult(
                loadSessionId, success, totalRows, stagedRows, loadedRows, errorRows, message
        );
    }

    void validatePublishCounters(long loadSessionId, long totalRows, long stagedRows) {
        if (stagedRows != totalRows) {
            throw new IllegalStateException(
                    "CDData processing counter mismatch. loadSessionId=" + loadSessionId
                            + ", totalRows=" + totalRows
                            + ", stagedRows=" + stagedRows
            );
        }
    }

    private long publish(long loadSessionId, long expectedRows) {
        long publishStartedAt = System.nanoTime();
        log.info("CDData publish started. loadSessionId={}, expectedRows={}", loadSessionId, expectedRows);
        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                int publishedRows = targetRepository.publishFromStage(connection, loadSessionId);
                if (publishedRows != expectedRows) {
                    throw new IllegalStateException(
                            "CDData publish row count mismatch. loadSessionId=" + loadSessionId
                                    + ", expectedRows=" + expectedRows
                                    + ", publishedRows=" + publishedRows
                    );
                }

                long cleanupStartedAt = System.nanoTime();
                int cleanedRows = stageRepository.deleteByLoadSessionId(connection, loadSessionId);
                if (cleanedRows != expectedRows) {
                    throw new IllegalStateException(
                            "CDData stage cleanup row count mismatch. loadSessionId=" + loadSessionId
                                    + ", expectedRows=" + expectedRows
                                    + ", cleanedRows=" + cleanedRows
                    );
                }
                log.info("CDData stage cleanup completed. loadSessionId={}, affectedRows={}, elapsedMs={}",
                        loadSessionId, cleanedRows, elapsedMs(cleanupStartedAt));

                long commitStartedAt = System.nanoTime();
                connection.commit();
                log.info("CDData publish commit completed. loadSessionId={}, elapsedMs={}",
                        loadSessionId, elapsedMs(commitStartedAt));
                return publishedRows;
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                log.info("CDData publish rollback. loadSessionId={}, reason={}", loadSessionId, e.getMessage());
                throw processingFailure("Failed to publish CDData", loadSessionId, e);
            } finally {
                restoreAutoCommitQuietly(connection, oldAutoCommit);
            }
        } catch (SQLException e) {
            throw processingFailure("Failed to open CDData publish transaction", loadSessionId, e);
        } finally {
            log.info("CDData publish finished. loadSessionId={}, elapsedMs={}",
                    loadSessionId, elapsedMs(publishStartedAt));
        }
    }

    private void cleanupPreviousProcessing(long loadSessionId) {
        long startedAt = System.nanoTime();
        log.info("CDData stage/error cleanup started. loadSessionId={}", loadSessionId);
        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                stageRepository.deleteByLoadSessionId(connection, loadSessionId);
                errorRepository.deleteByLoadSessionId(connection, loadSessionId);
                connection.commit();
                log.info("CDData stage/error cleanup finished. loadSessionId={}, elapsedMs={}",
                        loadSessionId, elapsedMs(startedAt));
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                throw processingFailure("Failed to clean CDData staging", loadSessionId, e);
            } finally {
                restoreAutoCommitQuietly(connection, oldAutoCommit);
            }
        } catch (SQLException e) {
            throw processingFailure("Failed to open CDData cleanup transaction", loadSessionId, e);
        }
    }

    private void writeChunk(
            long loadSessionId,
            int chunkNumber,
            List<CDDataStageRow> stageRows,
            List<CDDataValidationError> errors
    ) {
        long startedAt = System.nanoTime();
        log.info("CDData chunk write started. loadSessionId={}, chunkNumber={}, stageRows={}, errorRows={}",
                loadSessionId, chunkNumber, stageRows.size(), errors.size());
        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                stageRepository.insertBatch(connection, loadSessionId, stageRows);
                errorRepository.insertBatch(connection, loadSessionId, errors);
                log.info("CDData chunk commit started. loadSessionId={}, chunkNumber={}",
                        loadSessionId, chunkNumber);
                connection.commit();
                log.info("CDData chunk commit finished. loadSessionId={}, chunkNumber={}",
                        loadSessionId, chunkNumber);
                log.info("CDData chunk write finished. loadSessionId={}, chunkNumber={}, elapsedMs={}",
                        loadSessionId, chunkNumber, elapsedMs(startedAt));
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                throw processingFailure("Failed to write CDData chunk " + chunkNumber, loadSessionId, e);
            } finally {
                restoreAutoCommitQuietly(connection, oldAutoCommit);
            }
        } catch (SQLException e) {
            throw processingFailure("Failed to open CDData chunk transaction", loadSessionId, e);
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
        log.info("CDData memory. loadSessionId={}, phase={}, usedMemoryMb={}, totalMemoryMb={}, freeMemoryMb={}, "
                        + "maxMemoryMb={}",
                loadSessionId, phase, (totalMemory - freeMemory) / BYTES_PER_MB,
                totalMemory / BYTES_PER_MB, freeMemory / BYTES_PER_MB, maxMemory / BYTES_PER_MB);
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
