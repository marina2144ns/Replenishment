package ru.stockmann.replenishment.services.cddata.process;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDDataProcessorTest {

    @Test
    void blankNumericMetricsStageAsZeroAndPublish() {
        TestContext context = TestContext.withChunks(List.of(actualRow(1, null)));

        CDDataProcessResult result = context.processor(new CDDataValidator()).process(100L);

        assertTrue(result.success());
        assertEquals(1, context.targetRepository.publishCalls);
        CDDataStageRow row = context.stageRepository.insertedRows.get(0);
        for (BigDecimal value : List.of(
                row.stockStartPcs(), row.stockStartDd(), row.salesPcs(), row.salesRub(), row.revenue(),
                row.gp(), row.cogs(), row.salesFrpPrice(), row.salesDiscount(), row.stockStoresPcs(),
                row.stockStoresDd()
        )) {
            assertEquals(0, value.compareTo(BigDecimal.ZERO));
        }
        assertEquals(0, row.planRub());
    }

    @Test
    void invalidNumericMetricPersistsErrorAndPreventsTargetPublish() {
        TestContext context = TestContext.withChunks(List.of(actualRow(1, "abc")));

        CDDataProcessResult result = context.processor(new CDDataValidator()).process(100L);

        assertFalse(result.success());
        assertEquals(1, result.errorRows());
        assertEquals(0, context.targetRepository.publishCalls);
        assertTrue(context.stageRepository.insertedRows.isEmpty());
    }

    @Test
    void processesMultipleValidChunksAndPublishesTargetAtomically() {
        TestContext context = TestContext.withChunks(
                List.of(row(1), row(2)),
                List.of(row(3))
        );

        CDDataProcessResult result = context.processor().process(100L);

        assertTrue(result.success());
        assertEquals(3, result.totalRows());
        assertEquals(3, result.stagedRows());
        assertEquals(3, result.loadedRows());
        assertEquals(0, result.errorRows());
        assertEquals(List.of(0L, 2L, 3L), context.rawRepository.lastRawIds);
        assertEquals(List.of(List.of(1L, 2L), List.of(3L)), context.stageRepository.insertedRawIds);
        assertEquals(4, context.connection.commits);
        assertEquals(0, context.connection.rollbacks);
        assertEquals(1, context.targetRepository.publishCalls);
        assertEquals(3, context.stageRepository.publishCleanupRows);
    }

    @Test
    void writesErrorsFromDifferentChunksAndStagesOnlyValidRows() {
        TestContext context = TestContext.withChunks(
                List.of(row(1), row(2)),
                List.of(row(3), row(4))
        );
        context.validator.errorFields.put(2L, List.of("god"));
        context.validator.errorFields.put(4L, List.of("data", "planRub"));

        CDDataProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals(4, result.totalRows());
        assertEquals(2, result.stagedRows());
        assertEquals(0, result.loadedRows());
        assertEquals(3, result.errorRows());
        assertEquals(List.of(List.of(1L), List.of(3L)), context.stageRepository.insertedRawIds);
        assertEquals(List.of(List.of(2L), List.of(4L, 4L)), context.errorRepository.insertedRawIds);
        assertEquals(3, context.connection.commits);
        assertEquals(0, context.targetRepository.publishCalls);
        assertEquals(1, context.stageRepository.deleteCalls);
    }

    @Test
    void missingRequiredDeleteKeyPersistsErrorAndPreventsTargetPublish() {
        TestContext context = TestContext.withChunks(List.of(row(1)));
        context.validator.errorFields.put(1L, List.of("god"));

        CDDataProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals(1, result.errorRows());
        assertEquals(List.of(List.of(1L)), context.errorRepository.insertedRawIds);
        assertTrue(context.stageRepository.insertedRawIds.stream().allMatch(List::isEmpty));
        assertEquals(0, context.targetRepository.publishCalls);
    }

    @Test
    void missingOrWrongLoadTypeDoesNotOpenTransactions() {
        TestContext context = TestContext.withChunks(List.of(row(1)));
        context.loadSessionRepository.exists = false;

        CDDataProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals(0, result.totalRows());
        assertEquals(0, result.stagedRows());
        assertEquals(0, result.loadedRows());
        assertEquals(1, result.errorRows());
        assertEquals(0, context.connection.commits);
        assertTrue(context.rawRepository.lastRawIds.isEmpty());
    }

    @Test
    void chunkSqlFailureRollsBackOnlyCurrentChunkAndStopsProcessing() {
        TestContext context = TestContext.withChunks(
                List.of(row(1)),
                List.of(row(2))
        );
        context.stageRepository.failOnInsertCall = 2;

        CDDataProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals(1, result.totalRows());
        assertEquals(1, result.stagedRows());
        assertEquals(0, result.loadedRows());
        assertEquals(1, result.errorRows());
        assertEquals(2, context.connection.commits);
        assertEquals(1, context.connection.rollbacks);
        assertEquals(List.of(0L, 1L), context.rawRepository.lastRawIds);
        assertEquals(1, context.errorRepository.processingErrors.size());
        assertEquals(0, context.targetRepository.publishCalls);
    }

    @Test
    void cleanupFailureRollsBackAndDoesNotReadRaw() {
        TestContext context = TestContext.withChunks(List.of(row(1)));
        context.stageRepository.deleteFailure = new RuntimeException("cleanup failed");

        CDDataProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals(0, result.totalRows());
        assertEquals(1, result.errorRows());
        assertEquals(0, context.connection.commits);
        assertEquals(1, context.connection.rollbacks);
        assertTrue(context.rawRepository.lastRawIds.isEmpty());
    }

    @Test
    void repeatedProcessingCleansStageAndErrorsBeforeReadingChunks() {
        TestContext context = TestContext.withChunks(List.of(row(1)));

        context.processor().process(100L);
        context.rawRepository.reset();
        context.processor().process(100L);

        assertEquals(4, context.stageRepository.deleteCalls);
        assertEquals(2, context.errorRepository.deleteCalls);
        assertEquals(List.of("deleteStage", "deleteErrors", "read:0"), context.events.subList(0, 3));
        int secondErrorCleanup = context.events.lastIndexOf("deleteErrors");
        assertEquals("deleteStage", context.events.get(secondErrorCleanup - 1));
        assertEquals("read:0", context.events.get(secondErrorCleanup + 1));
    }

    @Test
    void emptySessionCommitsCleanupAndReturnsZeroCounters() {
        TestContext context = TestContext.withChunks();

        CDDataProcessResult result = context.processor().process(100L);

        assertTrue(result.success());
        assertEquals(0, result.totalRows());
        assertEquals(0, result.stagedRows());
        assertEquals(0, result.loadedRows());
        assertEquals(0, result.errorRows());
        assertEquals(2, context.connection.commits);
        assertEquals(1, context.targetRepository.publishCalls);
    }

    @Test
    void publishFailureRollsBackAndReturnsProcessingFailure() {
        TestContext context = TestContext.withChunks(List.of(row(1)));
        context.targetRepository.failure = new RuntimeException("publish failed");

        CDDataProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals(1, result.totalRows());
        assertEquals(1, result.stagedRows());
        assertEquals(0, result.loadedRows());
        assertEquals(1, result.errorRows());
        assertEquals(1, context.connection.rollbacks);
        assertEquals(1, context.stageRepository.deleteCalls);
    }

    @Test
    void publishedRowCountMismatchRollsBackWithoutStageCleanup() {
        TestContext context = TestContext.withChunks(List.of(row(1)));
        context.targetRepository.publishedRows = 0;

        CDDataProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals(0, result.loadedRows());
        assertEquals(1, context.connection.rollbacks);
        assertEquals(1, context.stageRepository.deleteCalls);
    }

    @Test
    void stageCleanupFailureAndCountMismatchRollbackPublish() {
        TestContext cleanupFailure = TestContext.withChunks(List.of(row(1)));
        cleanupFailure.stageRepository.failPublishCleanup = true;

        CDDataProcessResult failure = cleanupFailure.processor().process(100L);

        assertFalse(failure.success());
        assertEquals(1, cleanupFailure.connection.rollbacks);

        TestContext countMismatch = TestContext.withChunks(List.of(row(1)));
        countMismatch.stageRepository.cleanupCountOverride = 0;

        CDDataProcessResult mismatch = countMismatch.processor().process(100L);

        assertFalse(mismatch.success());
        assertEquals(1, countMismatch.connection.rollbacks);
    }

    @Test
    void publishCommitFailureRollsBackAndDoesNotReturnLoadedRows() {
        TestContext context = TestContext.withChunks(List.of(row(1)));
        context.connection.failCommitNumber = 3;

        CDDataProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals(0, result.loadedRows());
        assertEquals(1, result.errorRows());
        assertEquals(1, context.connection.rollbacks);
    }

    @Test
    void counterMismatchIsRejectedBeforePublish() {
        TestContext context = TestContext.withChunks();

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                context.processor().validatePublishCounters(100L, 10L, 9L));

        assertTrue(error.getMessage().contains("counter mismatch"));
        assertEquals(0, context.targetRepository.publishCalls);
    }

    private static CDDataRawRow row(long id) {
        return new CDDataRawRow(
                id, 100L, id + 10, "row-" + id,
                null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null
        );
    }

    private static CDDataRawRow actualRow(long id, String salesRub) {
        return new CDDataRawRow(
                id, 100L, id + 10, "Name", "2025", "1", "1", null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null,
                null, null, null, salesRub, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null
        );
    }

    private static CDDataStageRow stageRow(CDDataRawRow row) {
        return new CDDataStageRow(
                row.loadSessionId(), row.excelRowNum(), row.nazvanie(),
                null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                row.id()
        );
    }

    private static final class TestContext {
        private final List<String> events = new ArrayList<>();
        private final RecordingConnection connection = new RecordingConnection();
        private final FakeLoadSessionRepository loadSessionRepository = new FakeLoadSessionRepository();
        private final FakeRawRepository rawRepository;
        private final FakeErrorRepository errorRepository = new FakeErrorRepository(events);
        private final FakeStageRepository stageRepository = new FakeStageRepository(events);
        private final FakeTargetRepository targetRepository;
        private final FakeValidator validator = new FakeValidator();

        private TestContext(List<List<CDDataRawRow>> chunks) {
            rawRepository = new FakeRawRepository(events, chunks);
            targetRepository = new FakeTargetRepository(
                    chunks.stream().mapToInt(List::size).sum()
            );
        }

        @SafeVarargs
        static TestContext withChunks(List<CDDataRawRow>... chunks) {
            return new TestContext(List.of(chunks));
        }

        CDDataProcessor processor() {
            return processor(validator);
        }

        CDDataProcessor processor(CDDataValidator selectedValidator) {
            return new CDDataProcessor(
                    dataSource(connection),
                    loadSessionRepository,
                    rawRepository,
                    errorRepository,
                    stageRepository,
                    targetRepository,
                    selectedValidator
            );
        }
    }

    private static DataSource dataSource(RecordingConnection recording) {
        return (DataSource) Proxy.newProxyInstance(
                DataSource.class.getClassLoader(),
                new Class<?>[]{DataSource.class},
                (proxy, method, args) -> "getConnection".equals(method.getName())
                        ? recording.connection()
                        : defaultValue(method.getReturnType())
        );
    }

    private static final class FakeLoadSessionRepository extends CDDataLoadSessionRepository {
        private boolean exists = true;

        private FakeLoadSessionRepository() {
            super(null);
        }

        @Override
        public boolean existsById(long loadSessionId) {
            return exists;
        }
    }

    private static final class FakeRawRepository extends CDDataRawRepository {
        private final List<String> events;
        private final List<List<CDDataRawRow>> chunks;
        private final List<Long> lastRawIds = new ArrayList<>();
        private int call;

        private FakeRawRepository(List<String> events, List<List<CDDataRawRow>> chunks) {
            super(null);
            this.events = events;
            this.chunks = chunks;
        }

        @Override
        public List<CDDataRawRow> findChunk(long loadSessionId, long lastRawId) {
            events.add("read:" + lastRawId);
            lastRawIds.add(lastRawId);
            return call < chunks.size() ? chunks.get(call++) : List.of();
        }

        void reset() {
            call = 0;
            lastRawIds.clear();
        }

        @Override
        public List<CDDataRawRow> findByLoadSessionId(Connection connection, long loadSessionId) {
            throw new AssertionError("find-all raw method must not be used");
        }
    }

    private static final class FakeStageRepository extends CDDataStageRepository {
        private final List<String> events;
        private final List<List<Long>> insertedRawIds = new ArrayList<>();
        private final List<CDDataStageRow> insertedRows = new ArrayList<>();
        private int deleteCalls;
        private int insertCalls;
        private int failOnInsertCall;
        private RuntimeException deleteFailure;
        private boolean failPublishCleanup;
        private Integer cleanupCountOverride;
        private int publishCleanupRows;

        private FakeStageRepository(List<String> events) {
            this.events = events;
        }

        @Override
        public int deleteByLoadSessionId(Connection connection, long loadSessionId) {
            events.add("deleteStage");
            deleteCalls++;
            if (deleteFailure != null) {
                throw deleteFailure;
            }
            if (deleteCalls > 1) {
                if (failPublishCleanup) {
                    throw new RuntimeException("stage cleanup failed");
                }
                int rows = insertedRawIds.stream().mapToInt(List::size).sum();
                publishCleanupRows = cleanupCountOverride == null ? rows : cleanupCountOverride;
                return publishCleanupRows;
            }
            return 0;
        }

        @Override
        public void insertBatch(Connection connection, long loadSessionId, List<CDDataStageRow> rows) {
            insertCalls++;
            if (failOnInsertCall == insertCalls) {
                throw new RuntimeException("stage insert failed");
            }
            insertedRawIds.add(rows.stream().map(row -> row.excelRowNum() - 10).toList());
            insertedRows.addAll(rows);
        }
    }

    private static final class FakeTargetRepository extends CDDataTargetRepository {
        private int publishedRows;
        private int publishCalls;
        private RuntimeException failure;

        private FakeTargetRepository(int publishedRows) {
            this.publishedRows = publishedRows;
        }

        @Override
        public int publishFromStage(Connection connection, long loadSessionId) {
            publishCalls++;
            if (failure != null) {
                throw failure;
            }
            return publishedRows;
        }
    }

    private static final class FakeErrorRepository extends CDDataErrorRepository {
        private final List<String> events;
        private final List<List<Long>> insertedRawIds = new ArrayList<>();
        private final List<CDDataValidationError> processingErrors = new ArrayList<>();
        private int deleteCalls;

        private FakeErrorRepository(List<String> events) {
            super(null);
            this.events = events;
        }

        @Override
        public void deleteByLoadSessionId(Connection connection, long loadSessionId) {
            events.add("deleteErrors");
            deleteCalls++;
        }

        @Override
        public void insertBatch(
                Connection connection,
                long loadSessionId,
                List<CDDataValidationError> errors
        ) {
            insertedRawIds.add(errors.stream().map(CDDataValidationError::rawId).toList());
        }

        @Override
        public void insertAll(List<CDDataValidationError> errors) {
            processingErrors.addAll(errors);
        }
    }

    private static final class FakeValidator extends CDDataValidator {
        private final Map<Long, List<String>> errorFields = new HashMap<>();

        @Override
        public CDDataRowValidationResult validateAndMap(CDDataRawRow row) {
            List<String> fields = errorFields.getOrDefault(row.id(), List.of());
            if (fields.isEmpty()) {
                return new CDDataRowValidationResult(stageRow(row), List.of());
            }
            return new CDDataRowValidationResult(null, fields.stream()
                    .map(field -> new CDDataValidationError(
                            row.loadSessionId(), row.id(), row.excelRowNum(), "VALIDATION",
                            field, "INVALID_VALUE", "reason", "message"
                    ))
                    .toList());
        }
    }

    private static final class RecordingConnection {
        private boolean autoCommit = true;
        private int commits;
        private int rollbacks;
        private int failCommitNumber;

        Connection connection() {
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getAutoCommit" -> autoCommit;
                        case "setAutoCommit" -> {
                            autoCommit = (Boolean) args[0];
                            yield null;
                        }
                        case "commit" -> {
                            if (commits + 1 == failCommitNumber) {
                                throw new SQLException("commit failed");
                            }
                            commits++;
                            yield null;
                        }
                        case "rollback" -> {
                            rollbacks++;
                            yield null;
                        }
                        default -> defaultValue(method.getReturnType());
                    }
            );
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
