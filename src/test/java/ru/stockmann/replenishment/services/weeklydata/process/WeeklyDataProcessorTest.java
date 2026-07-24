package ru.stockmann.replenishment.services.weeklydata.process;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyDataProcessorTest {

    private static final long LOAD_SESSION_ID = 10L;

    @Test
    void sessionNotFoundDoesNotReadOrClean() {
        TestFixture fixture = new TestFixture();
        fixture.loadSessionRepository.exists = false;

        WeeklyDataProcessResult result = fixture.processor.process(LOAD_SESSION_ID);

        assertFalse(result.success());
        assertEquals(0, fixture.rawRepository.findCalls);
        assertEquals(0, fixture.stageRepository.deleteCalls);
        assertEquals(0, fixture.errorRepository.deleteCalls);
        assertEquals(0, fixture.dataSource.commitCount);
    }

    @Test
    void processesMultipleValidChunksAndCommitsCleanupAndEachChunk() {
        TestFixture fixture = new TestFixture();
        fixture.rawRepository.chunks.put(0L, List.of(validRow(1), validRow(2)));
        fixture.rawRepository.chunks.put(2L, List.of(validRow(3)));

        WeeklyDataProcessResult result = fixture.processor.process(LOAD_SESSION_ID);

        assertTrue(result.success());
        assertEquals(3, result.totalRows());
        assertEquals(3, result.stagedRows());
        assertEquals(3, result.loadedRows());
        assertEquals(0, result.errorRows());
        assertEquals(List.of(0L, 2L, 3L), fixture.rawRepository.requestedLastIds);
        assertEquals(List.of(2, 1), fixture.stageRepository.batchSizes);
        assertEquals(0, fixture.errorRepository.validationErrors.size());
        assertEquals(4, fixture.dataSource.commitCount);
        assertEquals(0, fixture.dataSource.rollbackCount);
        assertEquals(1, fixture.targetRepository.publishCalls);
        assertTrue(fixture.stageRepository.rows.isEmpty());
    }

    @Test
    void writesErrorsFromDifferentChunksAndStagesOnlyValidRows() {
        TestFixture fixture = new TestFixture();
        fixture.rawRepository.chunks.put(0L, List.of(validRow(1), invalidRow(2)));
        fixture.rawRepository.chunks.put(2L, List.of(invalidRow(3), validRow(4)));

        WeeklyDataProcessResult result = fixture.processor.process(LOAD_SESSION_ID);

        assertFalse(result.success());
        assertEquals(4, result.totalRows());
        assertEquals(2, result.stagedRows());
        assertEquals(0, result.loadedRows());
        assertEquals(2, result.errorRows());
        assertEquals(List.of(1, 1), fixture.stageRepository.batchSizes);
        assertEquals(List.of(1, 1), fixture.errorRepository.batchSizes);
        assertEquals(List.of(2L, 3L), fixture.errorRepository.validationErrors.stream()
                .map(WeeklyDataValidationError::rawId)
                .toList());
        assertEquals(List.of(3L, 4L), fixture.errorRepository.validationErrors.stream()
                .map(WeeklyDataValidationError::excelRowNum)
                .toList());
        assertEquals(3, fixture.dataSource.commitCount);
        assertEquals(0, fixture.targetRepository.publishCalls);
        assertEquals(2, fixture.stageRepository.rows.size());
    }

    @Test
    void rollsBackOnlyFailingChunkAndStopsProcessing() {
        TestFixture fixture = new TestFixture();
        fixture.rawRepository.chunks.put(0L, List.of(validRow(1)));
        fixture.rawRepository.chunks.put(1L, List.of(validRow(2)));
        fixture.stageRepository.failOnBatchNumber = 2;

        WeeklyDataProcessResult result = fixture.processor.process(LOAD_SESSION_ID);

        assertFalse(result.success());
        assertEquals(1, result.totalRows());
        assertEquals(1, result.stagedRows());
        assertEquals(0, result.loadedRows());
        assertEquals(1, result.errorRows());
        assertEquals(2, fixture.dataSource.commitCount);
        assertEquals(1, fixture.dataSource.rollbackCount);
        assertEquals(List.of(0L, 1L), fixture.rawRepository.requestedLastIds);
        assertEquals(1, fixture.errorRepository.processingErrors.size());
    }

    @Test
    void everyRunCleansPreviouslyCommittedStageAndErrorsFirst() {
        TestFixture fixture = new TestFixture();

        fixture.processor.process(LOAD_SESSION_ID);
        fixture.processor.process(LOAD_SESSION_ID);

        assertEquals(4, fixture.stageRepository.deleteCalls);
        assertEquals(2, fixture.errorRepository.deleteCalls);
        assertEquals(4, fixture.dataSource.commitCount);
    }

    @Test
    void counterMismatchPreventsPublishAndCreatesProcessingError() {
        TestFixture fixture = new TestFixture();
        List<WeeklyDataRawRow> inconsistentChunk = new java.util.AbstractList<>() {
            @Override
            public WeeklyDataRawRow get(int index) {
                return index == 0 ? validRow(1) : validRow(2);
            }

            @Override
            public int size() {
                return 2;
            }

            @Override
            public java.util.Iterator<WeeklyDataRawRow> iterator() {
                return List.of(validRow(1)).iterator();
            }
        };
        fixture.rawRepository.chunks.put(0L, inconsistentChunk);

        WeeklyDataProcessResult result = fixture.processor.process(LOAD_SESSION_ID);

        assertFalse(result.success());
        assertEquals(2, result.totalRows());
        assertEquals(1, result.stagedRows());
        assertEquals(0, result.loadedRows());
        assertEquals(0, fixture.targetRepository.publishCalls);
        assertEquals(1, fixture.errorRepository.processingErrors.size());
    }

    @Test
    void publishFailureRollsBackAndLeavesStageAvailable() {
        TestFixture fixture = new TestFixture();
        fixture.rawRepository.chunks.put(0L, List.of(validRow(1)));
        fixture.targetRepository.failure = new RuntimeException("publish failed");

        WeeklyDataProcessResult result = fixture.processor.process(LOAD_SESSION_ID);

        assertFalse(result.success());
        assertEquals(0, result.loadedRows());
        assertEquals(1, fixture.dataSource.rollbackCount);
        assertEquals(1, fixture.stageRepository.rows.size());
        assertEquals(1, fixture.errorRepository.processingErrors.size());
    }

    @Test
    void publishedRowMismatchRollsBackAndLeavesStageAvailable() {
        TestFixture fixture = new TestFixture();
        fixture.rawRepository.chunks.put(0L, List.of(validRow(1)));
        fixture.targetRepository.publishedRowsOverride = 0;

        WeeklyDataProcessResult result = fixture.processor.process(LOAD_SESSION_ID);

        assertFalse(result.success());
        assertEquals(1, fixture.dataSource.rollbackCount);
        assertEquals(1, fixture.stageRepository.rows.size());
    }

    @Test
    void stageCleanupFailureRollsBackPublishAndLeavesStageAvailable() {
        TestFixture fixture = new TestFixture();
        fixture.rawRepository.chunks.put(0L, List.of(validRow(1)));
        fixture.stageRepository.failOnDeleteCall = 2;

        WeeklyDataProcessResult result = fixture.processor.process(LOAD_SESSION_ID);

        assertFalse(result.success());
        assertEquals(1, fixture.dataSource.rollbackCount);
        assertEquals(1, fixture.stageRepository.rows.size());
    }

    @Test
    void publishCommitFailureRollsBackAndDoesNotReturnSuccess() {
        TestFixture fixture = new TestFixture();
        fixture.rawRepository.chunks.put(0L, List.of(validRow(1)));
        fixture.dataSource.failCommitNumber = 3;

        WeeklyDataProcessResult result = fixture.processor.process(LOAD_SESSION_ID);

        assertFalse(result.success());
        assertEquals(1, fixture.dataSource.rollbackCount);
        assertEquals(0, result.loadedRows());
    }

    @Test
    void processorSourceUsesChunkApiAndOnlySetBasedTargetPublish() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/ru/stockmann/replenishment/services/weeklydata/process/WeeklyDataProcessor.java"
        ));
        String targetSource = Files.readString(Path.of(
                "src/main/java/ru/stockmann/replenishment/services/weeklydata/process/WeeklyDataTargetRepository.java"
        ));

        assertTrue(source.contains("rawRepository.findChunk("));
        assertFalse(source.contains("findByLoadSessionId("));
        assertFalse(source.contains("List<WeeklyDataTargetRow>"));
        assertTrue(source.contains("targetRepository.publishFromStage("));
        assertFalse(targetSource.contains("addBatch("));
        assertFalse(targetSource.contains("executeBatch("));
    }

    private static WeeklyDataRawRow validRow(long rawId) {
        return row(rawId, "2025", "10");
    }

    private static WeeklyDataRawRow invalidRow(long rawId) {
        return row(rawId, null, "10");
    }

    private static WeeklyDataRawRow row(long rawId, String year, String week) {
        return new WeeklyDataRawRow(
                LOAD_SESSION_ID,
                rawId,
                rawId + 1,
                null,
                null,
                null,
                null,
                year,
                week,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static final class TestFixture {
        private final RecordingDataSource dataSource = new RecordingDataSource();
        private final FakeLoadSessionRepository loadSessionRepository = new FakeLoadSessionRepository();
        private final FakeRawRepository rawRepository = new FakeRawRepository();
        private final List<String> cleanupEvents = new ArrayList<>();
        private final FakeErrorRepository errorRepository = new FakeErrorRepository(cleanupEvents);
        private final FakeStageRepository stageRepository = new FakeStageRepository(cleanupEvents);
        private final FakeTargetRepository targetRepository = new FakeTargetRepository(stageRepository);
        private final WeeklyDataProcessor processor = new WeeklyDataProcessor(
                dataSource,
                loadSessionRepository,
                rawRepository,
                errorRepository,
                stageRepository,
                targetRepository,
                new WeeklyDataValidator()
        );
    }

    private static final class FakeLoadSessionRepository extends WeeklyDataLoadSessionRepository {
        private boolean exists = true;

        private FakeLoadSessionRepository() {
            super(null);
        }

        @Override
        public boolean existsById(long loadSessionId) {
            return exists;
        }
    }

    private static final class FakeRawRepository extends WeeklyDataRawRepository {
        private final Map<Long, List<WeeklyDataRawRow>> chunks = new LinkedHashMap<>();
        private final List<Long> requestedLastIds = new ArrayList<>();
        private int findCalls;

        private FakeRawRepository() {
            super(null);
        }

        @Override
        public List<WeeklyDataRawRow> findChunk(long loadSessionId, long lastRawId) {
            findCalls++;
            requestedLastIds.add(lastRawId);
            return chunks.getOrDefault(lastRawId, List.of());
        }
    }

    private static final class FakeStageRepository extends WeeklyDataStageRepository {
        private final List<String> cleanupEvents;
        private final List<WeeklyDataStageRow> rows = new ArrayList<>();
        private final List<Integer> batchSizes = new ArrayList<>();
        private int deleteCalls;
        private int insertCalls;
        private int failOnBatchNumber;
        private int failOnDeleteCall;

        private FakeStageRepository(List<String> cleanupEvents) {
            super(null);
            this.cleanupEvents = cleanupEvents;
        }

        @Override
        public int deleteByLoadSessionId(Connection connection, long loadSessionId) {
            deleteCalls++;
            cleanupEvents.add("stageDelete");
            if (deleteCalls == failOnDeleteCall) {
                throw new RuntimeException("stage cleanup failed");
            }
            int deleted = rows.size();
            rows.clear();
            return deleted;
        }

        @Override
        public void insertBatch(Connection connection, long loadSessionId, List<WeeklyDataStageRow> rows) {
            if (rows.isEmpty()) {
                return;
            }
            insertCalls++;
            if (insertCalls == failOnBatchNumber) {
                throw new RuntimeException("stage insert failed");
            }
            batchSizes.add(rows.size());
            this.rows.addAll(rows);
        }
    }

    private static final class FakeTargetRepository extends WeeklyDataTargetRepository {
        private final FakeStageRepository stageRepository;
        private int publishCalls;
        private RuntimeException failure;
        private Integer publishedRowsOverride;

        private FakeTargetRepository(FakeStageRepository stageRepository) {
            this.stageRepository = stageRepository;
        }

        @Override
        public int publishFromStage(Connection connection, long loadSessionId) {
            publishCalls++;
            if (failure != null) {
                throw failure;
            }
            return publishedRowsOverride != null ? publishedRowsOverride : stageRepository.rows.size();
        }
    }

    private static final class FakeErrorRepository extends WeeklyDataErrorRepository {
        private final List<String> cleanupEvents;
        private final List<WeeklyDataValidationError> validationErrors = new ArrayList<>();
        private final List<WeeklyDataValidationError> processingErrors = new ArrayList<>();
        private final List<Integer> batchSizes = new ArrayList<>();
        private int deleteCalls;

        private FakeErrorRepository(List<String> cleanupEvents) {
            super(null);
            this.cleanupEvents = cleanupEvents;
        }

        @Override
        public void deleteByLoadSessionId(Connection connection, long loadSessionId) {
            deleteCalls++;
            cleanupEvents.add("errorDelete");
        }

        @Override
        public void insertBatch(
                Connection connection,
                long loadSessionId,
                List<WeeklyDataValidationError> errors
        ) {
            if (!errors.isEmpty()) {
                batchSizes.add(errors.size());
                validationErrors.addAll(errors);
            }
        }

        @Override
        public void insertAll(List<WeeklyDataValidationError> errors) {
            processingErrors.addAll(errors);
        }
    }

    private static final class RecordingDataSource implements DataSource {
        private int commitCount;
        private int rollbackCount;
        private int failCommitNumber;

        @Override
        public Connection getConnection() {
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getAutoCommit" -> true;
                        case "setAutoCommit", "close" -> null;
                        case "commit" -> {
                            commitCount++;
                            if (commitCount == failCommitNumber) {
                                throw new SQLException("commit failed");
                            }
                            yield null;
                        }
                        case "rollback" -> {
                            rollbackCount++;
                            yield null;
                        }
                        case "isClosed" -> false;
                        case "unwrap" -> null;
                        case "isWrapperFor" -> false;
                        default -> defaultValue(method.getReturnType());
                    }
            );
        }

        @Override
        public Connection getConnection(String username, String password) {
            return getConnection();
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("Not a wrapper");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == long.class) {
            return 0L;
        }
        return 0;
    }
}
