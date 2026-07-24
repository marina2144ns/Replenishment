package ru.stockmann.replenishment.services.cdecom.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CDEcomProcessor {

    private static final Logger log = LoggerFactory.getLogger(CDEcomProcessor.class);
    private static final int RAW_CHUNK_SIZE = CDEcomRawRepository.DEFAULT_CHUNK_SIZE;
    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final DataSource dataSource;
    private final CDEcomLoadSessionRepository loadSessionRepository;
    private final CDEcomRawRepository rawRepository;
    private final CDEcomErrorRepository errorRepository;
    private final CDEcomStageRepository stageRepository;
    private final CDEcomTargetRepository targetRepository;
    private final CDEcomValidator validator;

    public CDEcomProcessor(
            DataSource dataSource,
            CDEcomLoadSessionRepository loadSessionRepository,
            CDEcomRawRepository rawRepository,
            CDEcomErrorRepository errorRepository,
            CDEcomStageRepository stageRepository,
            CDEcomTargetRepository targetRepository,
            CDEcomValidator validator
    ) {
        this.dataSource = dataSource;
        this.loadSessionRepository = loadSessionRepository;
        this.rawRepository = rawRepository;
        this.errorRepository = errorRepository;
        this.stageRepository = stageRepository;
        this.targetRepository = targetRepository;
        this.validator = validator;
    }

    public CDEcomProcessResult process(long loadSessionId) {
        long processorStartedAt = System.nanoTime();
        long totalRows = 0;
        long stagedRows = 0;
        long loadedRows = 0;
        long errorRows = 0;

        log.info("CDEcom processing started. loadSessionId={}, chunkSize={}", loadSessionId, RAW_CHUNK_SIZE);
        try {
            long phaseStartedAt = System.nanoTime();
            log.info("CDEcom load session check started. loadSessionId={}", loadSessionId);
            boolean loadSessionExists = loadSessionRepository.existsById(loadSessionId);
            log.info("CDEcom load session check finished. loadSessionId={}, elapsedMs={}, exists={}",
                    loadSessionId, elapsedMs(phaseStartedAt), loadSessionExists);
            if (!loadSessionExists) {
                errorRows = 1;
                return result(loadSessionId, false, 0, 0, 0, errorRows,
                        "Load session not found or has unexpected LoadTypeCode. loadSessionId="
                                + loadSessionId + ", expected LoadTypeCode=CD_ECOM");
            }

            cleanupPreviousProcessing(loadSessionId);
            logMemory("cleanup", loadSessionId);

            long lastRawId = CDEcomRawRepository.INITIAL_LAST_RAW_ID;
            int chunkNumber = 0;
            while (true) {
                int nextChunkNumber = chunkNumber + 1;
                phaseStartedAt = System.nanoTime();
                log.info("CDEcom raw chunk read started. loadSessionId={}, chunkNumber={}, lastRawId={}, "
                                + "chunkSize={}",
                        loadSessionId, nextChunkNumber, lastRawId, RAW_CHUNK_SIZE);
                List<CDEcomRawRow> rawChunk = rawRepository.findChunk(loadSessionId, lastRawId);
                log.info("CDEcom raw chunk read finished. loadSessionId={}, chunkNumber={}, elapsedMs={}, rows={}",
                        loadSessionId, nextChunkNumber, elapsedMs(phaseStartedAt), rawChunk.size());
                if (rawChunk.isEmpty()) {
                    break;
                }

                chunkNumber++;
                long chunkStartedAt = System.nanoTime();
                List<CDEcomStageRow> stageRows = new ArrayList<>(rawChunk.size());
                List<CDEcomValidationError> errors = new ArrayList<>();

                phaseStartedAt = System.nanoTime();
                log.info("CDEcom validation and typing started. loadSessionId={}, chunkNumber={}, rows={}",
                        loadSessionId, chunkNumber, rawChunk.size());
                for (CDEcomRawRow rawRow : rawChunk) {
                    CDEcomRowValidationResult validationResult = validator.validateAndMap(rawRow);
                    if (validationResult.valid()) {
                        stageRows.add(validationResult.stageRow());
                    } else {
                        errors.addAll(validationResult.errors());
                    }
                }
                log.info("CDEcom validation and typing finished. loadSessionId={}, chunkNumber={}, elapsedMs={}, "
                                + "rows={}, stagedRows={}, errorRows={}",
                        loadSessionId, chunkNumber, elapsedMs(phaseStartedAt), rawChunk.size(),
                        stageRows.size(), errors.size());

                writeChunk(loadSessionId, chunkNumber, stageRows, errors);

                lastRawId = rawChunk.get(rawChunk.size() - 1).id();
                totalRows += rawChunk.size();
                stagedRows += stageRows.size();
                errorRows += errors.size();
                log.info("CDEcom chunk completed. loadSessionId={}, chunkNumber={}, lastRawId={}, chunkRows={}, "
                                + "cumulativeTotalRows={}, cumulativeStagedRows={}, cumulativeErrorRows={}, "
                                + "elapsedMs={}, usedMemoryMb={}",
                        loadSessionId, chunkNumber, lastRawId, rawChunk.size(), totalRows, stagedRows,
                        errorRows, elapsedMs(chunkStartedAt), usedMemoryMb());
            }

            log.info("CDEcom processing counters. loadSessionId={}, totalRows={}, stagedRows={}, loadedRows=0, "
                            + "errorRows={}",
                    loadSessionId, totalRows, stagedRows, errorRows);
            if (errorRows > 0) {
                return result(loadSessionId, false, totalRows, stagedRows, 0, errorRows,
                        "Validation failed; target was not changed");
            }
            validatePublishCounters(loadSessionId, totalRows, stagedRows);
            loadedRows = publish(loadSessionId, stagedRows);
            return result(loadSessionId, true, totalRows, stagedRows, loadedRows, 0,
                    "CDEcom load session processed and published successfully");
        } catch (RuntimeException e) {
            log.info("CDEcom processing failed. loadSessionId={}, reason={}", loadSessionId, e.getMessage());
            insertUnexpectedProcessingError(loadSessionId, e);
            errorRows++;
            return result(loadSessionId, false, totalRows, stagedRows, loadedRows, errorRows, e.getMessage());
        } finally {
            log.info("CDEcom processing finished. loadSessionId={}, elapsedMs={}, totalRows={}, stagedRows={}, "
                            + "loadedRows={}, errorRows={}",
                    loadSessionId, elapsedMs(processorStartedAt), totalRows, stagedRows, loadedRows, errorRows);
            logMemory("processor finish", loadSessionId);
        }
    }

    private CDEcomProcessResult result(
            long loadSessionId,
            boolean success,
            long totalRows,
            long stagedRows,
            long loadedRows,
            long errorRows,
            String message
    ) {
        return new CDEcomProcessResult(
                loadSessionId, success, totalRows, stagedRows, loadedRows, errorRows, message
        );
    }

    void validatePublishCounters(long loadSessionId, long totalRows, long stagedRows) {
        if (stagedRows != totalRows) {
            throw new IllegalStateException(
                    "CDEcom processing counter mismatch. loadSessionId=" + loadSessionId
                            + ", totalRows=" + totalRows
                            + ", stagedRows=" + stagedRows
            );
        }
    }

    private long publish(long loadSessionId, long expectedRows) {
        long publishStartedAt = System.nanoTime();
        log.info("CDEcom publish started. loadSessionId={}, expectedRows={}", loadSessionId, expectedRows);
        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                int publishedRows = targetRepository.publishFromStage(connection, loadSessionId);
                if (publishedRows != expectedRows) {
                    throw new IllegalStateException(
                            "CDEcom publish row count mismatch. loadSessionId=" + loadSessionId
                                    + ", expectedRows=" + expectedRows
                                    + ", publishedRows=" + publishedRows
                    );
                }

                long cleanupStartedAt = System.nanoTime();
                int cleanedRows = stageRepository.deleteByLoadSessionId(connection, loadSessionId);
                if (cleanedRows != expectedRows) {
                    throw new IllegalStateException(
                            "CDEcom stage cleanup row count mismatch. loadSessionId=" + loadSessionId
                                    + ", expectedRows=" + expectedRows
                                    + ", cleanedRows=" + cleanedRows
                    );
                }
                log.info("CDEcom stage cleanup completed. loadSessionId={}, affectedRows={}, elapsedMs={}",
                        loadSessionId, cleanedRows, elapsedMs(cleanupStartedAt));

                long commitStartedAt = System.nanoTime();
                connection.commit();
                log.info("CDEcom publish commit completed. loadSessionId={}, elapsedMs={}",
                        loadSessionId, elapsedMs(commitStartedAt));
                return publishedRows;
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                log.info("CDEcom publish rollback. loadSessionId={}, reason={}", loadSessionId, e.getMessage());
                throw processingFailure("Failed to publish CDEcom", loadSessionId, e);
            } finally {
                restoreAutoCommitQuietly(connection, oldAutoCommit);
            }
        } catch (SQLException e) {
            throw processingFailure("Failed to open CDEcom publish transaction", loadSessionId, e);
        } finally {
            log.info("CDEcom publish finished. loadSessionId={}, elapsedMs={}",
                    loadSessionId, elapsedMs(publishStartedAt));
        }
    }

    private void cleanupPreviousProcessing(long loadSessionId) {
        long startedAt = System.nanoTime();
        log.info("CDEcom stage/error cleanup started. loadSessionId={}", loadSessionId);
        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                stageRepository.deleteByLoadSessionId(connection, loadSessionId);
                errorRepository.deleteByLoadSessionId(connection, loadSessionId);
                connection.commit();
                log.info("CDEcom stage/error cleanup finished. loadSessionId={}, elapsedMs={}",
                        loadSessionId, elapsedMs(startedAt));
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                throw processingFailure("Failed to clean CDEcom staging", loadSessionId, e);
            } finally {
                restoreAutoCommitQuietly(connection, oldAutoCommit);
            }
        } catch (SQLException e) {
            throw processingFailure("Failed to open CDEcom cleanup transaction", loadSessionId, e);
        }
    }

    private void writeChunk(
            long loadSessionId,
            int chunkNumber,
            List<CDEcomStageRow> stageRows,
            List<CDEcomValidationError> errors
    ) {
        long startedAt = System.nanoTime();
        log.info("CDEcom chunk write started. loadSessionId={}, chunkNumber={}, stageRows={}, errorRows={}",
                loadSessionId, chunkNumber, stageRows.size(), errors.size());
        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                stageRepository.insertBatch(connection, loadSessionId, stageRows);
                errorRepository.insertBatch(connection, loadSessionId, errors);
                log.info("CDEcom chunk commit started. loadSessionId={}, chunkNumber={}",
                        loadSessionId, chunkNumber);
                connection.commit();
                log.info("CDEcom chunk commit finished. loadSessionId={}, chunkNumber={}",
                        loadSessionId, chunkNumber);
                log.info("CDEcom chunk write finished. loadSessionId={}, chunkNumber={}, elapsedMs={}",
                        loadSessionId, chunkNumber, elapsedMs(startedAt));
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                throw processingFailure("Failed to write CDEcom chunk " + chunkNumber, loadSessionId, e);
            } finally {
                restoreAutoCommitQuietly(connection, oldAutoCommit);
            }
        } catch (SQLException e) {
            throw processingFailure("Failed to open CDEcom chunk transaction", loadSessionId, e);
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
        log.info("CDEcom memory. loadSessionId={}, phase={}, usedMemoryMb={}, totalMemoryMb={}, freeMemoryMb={}, "
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
            errorRepository.insertAll(List.of(new CDEcomValidationError(
                    loadSessionId, 0L, null, "PROCESSING", null, "UNEXPECTED_PROCESSING_ERROR",
                    trim(message, 500), trim("Unexpected processing error: " + message, 4000)
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
