package ru.stockmann.replenishment.services.cdecom.process;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDEcomProcessorTest {

    @Test
    void processesMultipleValidChunksAndPublishesOnce() {
        TestContext context = TestContext.withChunks(List.of(row(1, "1")), List.of(row(2, "2")));

        CDEcomProcessResult result = context.processor().process(100L);

        assertTrue(result.success());
        assertEquals(2, result.totalRows());
        assertEquals(2, result.stagedRows());
        assertEquals(2, result.loadedRows());
        assertEquals(0, result.errorRows());
        assertEquals(List.of(0L, 1L, 2L), context.raw.lastRawIds);
        assertEquals(List.of(List.of(1L), List.of(2L)), context.stage.insertedExcelRows);
        assertEquals(4, context.connection.commits);
        assertEquals(0, context.connection.rollbacks);
        assertEquals(1, context.target.publishCalls);
        assertEquals(2, context.stage.deleteCalls);
    }

    @Test
    void errorsAcrossChunksAreWrittenAndOnlyValidRowsAreStaged() {
        TestContext context = TestContext.withChunks(
                List.of(row(1, "1"), row(2, "bad")),
                List.of(row(3, "3"), row(4, "bad"))
        );

        CDEcomProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals(4, result.totalRows());
        assertEquals(2, result.stagedRows());
        assertEquals(0, result.loadedRows());
        assertEquals(2, result.errorRows());
        assertEquals(List.of(List.of(1L), List.of(3L)), context.stage.insertedExcelRows);
        assertEquals(List.of(List.of(2L), List.of(4L)), context.errors.insertedExcelRows);
        assertEquals(3, context.connection.commits);
        assertEquals(0, context.target.publishCalls);
        assertEquals(1, context.stage.deleteCalls);
    }

    @Test
    void missingRequiredDeleteKeyPersistsErrorAndPreventsTargetPublish() {
        TestContext context = TestContext.withChunks(List.of(rowWithoutName(1)));

        CDEcomProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals(1, result.errorRows());
        assertEquals(List.of(List.of(1L)), context.errors.insertedExcelRows);
        assertTrue(context.stage.insertedExcelRows.stream().allMatch(List::isEmpty));
        assertEquals(0, context.target.publishCalls);
    }

    @Test
    void missingOrWrongLoadTypeDoesNotOpenProcessingTransaction() {
        TestContext context = TestContext.withChunks(List.of(row(1, "1")));
        context.session.exists = false;

        CDEcomProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals(0, result.totalRows());
        assertEquals(0, result.stagedRows());
        assertEquals(0, result.loadedRows());
        assertEquals(1, result.errorRows());
        assertEquals(0, context.connection.commits);
        assertTrue(context.raw.lastRawIds.isEmpty());
    }

    @Test
    void stageSqlFailureRollsBackCurrentChunkAndStops() {
        TestContext context = TestContext.withChunks(List.of(row(1, "1")), List.of(row(2, "2")));
        context.stage.failOnInsertCall = 2;

        CDEcomProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals(1, result.totalRows());
        assertEquals(1, result.stagedRows());
        assertEquals(0, result.loadedRows());
        assertEquals(1, context.connection.rollbacks);
        assertEquals(2, context.connection.commits);
        assertEquals(List.of(0L, 1L), context.raw.lastRawIds);
        assertEquals(1, context.errors.processingErrors.size());
    }

    @Test
    void errorSqlFailureRollsBackCurrentChunk() {
        TestContext context = TestContext.withChunks(List.of(row(1, "bad")));
        context.errors.failChunkInsert = true;

        CDEcomProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals(0, result.totalRows());
        assertEquals(1, result.errorRows());
        assertEquals(1, context.connection.rollbacks);
        assertEquals(1, context.connection.commits);
    }

    @Test
    void repeatProcessingCleansStageAndErrorsBeforeReadingRaw() {
        TestContext context = TestContext.withChunks(List.of(row(1, "1")));

        context.processor().process(100L);
        context.raw.reset();
        context.processor().process(100L);

        assertEquals(4, context.stage.deleteCalls);
        assertEquals(2, context.errors.deleteCalls);
        assertEquals(List.of("deleteStage", "deleteErrors", "read:0"), context.events.subList(0, 3));
        int secondCleanup = context.events.lastIndexOf("deleteErrors");
        assertEquals("deleteStage", context.events.get(secondCleanup - 1));
        assertEquals("read:0", context.events.get(secondCleanup + 1));
    }

    @Test
    void cleanupFailureRollsBackAndDoesNotReadRaw() {
        TestContext context = TestContext.withChunks(List.of(row(1, "1")));
        context.stage.deleteFailure = new RuntimeException("cleanup failed");

        CDEcomProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals(0, result.totalRows());
        assertEquals(1, context.connection.rollbacks);
        assertTrue(context.raw.lastRawIds.isEmpty());
    }

    @Test
    void publishFailureAndPublishedCountMismatchRollbackWithoutStageCleanup() {
        TestContext failure = TestContext.withChunks(List.of(row(1, "1")));
        failure.target.failure = new RuntimeException("publish failed");

        CDEcomProcessResult failed = failure.processor().process(100L);

        assertFalse(failed.success());
        assertEquals(0, failed.loadedRows());
        assertEquals(1, failure.connection.rollbacks);
        assertEquals(1, failure.stage.deleteCalls);

        TestContext mismatch = TestContext.withChunks(List.of(row(1, "1")));
        mismatch.target.publishedRowsOverride = 0;

        CDEcomProcessResult mismatched = mismatch.processor().process(100L);

        assertFalse(mismatched.success());
        assertEquals(0, mismatched.loadedRows());
        assertEquals(1, mismatch.connection.rollbacks);
        assertEquals(1, mismatch.stage.deleteCalls);
    }

    @Test
    void stageCleanupFailureAndMismatchRollbackPublish() {
        TestContext failure = TestContext.withChunks(List.of(row(1, "1")));
        failure.stage.failPublishCleanup = true;
        assertFalse(failure.processor().process(100L).success());
        assertEquals(1, failure.connection.rollbacks);

        TestContext mismatch = TestContext.withChunks(List.of(row(1, "1")));
        mismatch.stage.cleanupCountOverride = 0;
        assertFalse(mismatch.processor().process(100L).success());
        assertEquals(1, mismatch.connection.rollbacks);
    }

    @Test
    void publishCommitFailureRollsBackAndReturnsNoLoadedRows() {
        TestContext context = TestContext.withChunks(List.of(row(1, "1")));
        context.connection.failCommitNumber = 3;

        CDEcomProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals(0, result.loadedRows());
        assertEquals(1, result.errorRows());
        assertEquals(1, context.connection.rollbacks);
    }

    @Test
    void counterMismatchPreventsPublish() {
        TestContext context = TestContext.withChunks();

        try {
            context.processor().validatePublishCounters(100L, 10L, 9L);
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("counter mismatch"));
        }
        assertEquals(0, context.target.publishCalls);
    }

    private static CDEcomRawRow row(long id, String year) {
        return new CDEcomRawRow(
                id, 100L, id, "name", year, "1", "31", "31.01.2025",
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null
        );
    }

    private static CDEcomRawRow rowWithoutName(long id) {
        return new CDEcomRawRow(
                id, 100L, id, null, "2025", "1", "31", "31.01.2025",
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null
        );
    }

    private static final class TestContext {
        final List<String> events = new ArrayList<>();
        final RecordingConnection connection = new RecordingConnection();
        final FakeSession session = new FakeSession();
        final FakeRaw raw;
        final FakeStage stage = new FakeStage(events);
        final FakeErrors errors = new FakeErrors(events);
        final FakeTarget target = new FakeTarget(stage);

        TestContext(List<List<CDEcomRawRow>> chunks) {
            raw = new FakeRaw(events, chunks);
        }

        @SafeVarargs
        static TestContext withChunks(List<CDEcomRawRow>... chunks) {
            return new TestContext(List.of(chunks));
        }

        CDEcomProcessor processor() {
            return new CDEcomProcessor(
                    dataSource(connection), session, raw, errors, stage, target, new CDEcomValidator()
            );
        }
    }

    private static final class FakeSession extends CDEcomLoadSessionRepository {
        boolean exists = true;
        FakeSession() { super(null); }
        @Override public boolean existsById(long loadSessionId) { return exists; }
    }

    private static final class FakeRaw extends CDEcomRawRepository {
        final List<String> events;
        final List<List<CDEcomRawRow>> chunks;
        final List<Long> lastRawIds = new ArrayList<>();
        int index;
        FakeRaw(List<String> events, List<List<CDEcomRawRow>> chunks) {
            super(null);
            this.events = events;
            this.chunks = chunks;
        }
        @Override public List<CDEcomRawRow> findChunk(long loadSessionId, long lastRawId) {
            events.add("read:" + lastRawId);
            lastRawIds.add(lastRawId);
            return index < chunks.size() ? chunks.get(index++) : List.of();
        }
        void reset() { index = 0; lastRawIds.clear(); }
    }

    private static final class FakeStage extends CDEcomStageRepository {
        final List<String> events;
        final List<List<Long>> insertedExcelRows = new ArrayList<>();
        int deleteCalls;
        int insertCalls;
        int failOnInsertCall;
        RuntimeException deleteFailure;
        boolean failPublishCleanup;
        Integer cleanupCountOverride;
        boolean publishCleanupPending;
        int currentRows;
        FakeStage(List<String> events) { this.events = events; }
        @Override public int deleteByLoadSessionId(Connection connection, long loadSessionId) {
            events.add("deleteStage");
            deleteCalls++;
            if (deleteFailure != null) throw deleteFailure;
            if (publishCleanupPending) {
                if (failPublishCleanup) throw new RuntimeException("stage cleanup failed");
                int result = cleanupCountOverride == null ? currentRows : cleanupCountOverride;
                currentRows = 0;
                publishCleanupPending = false;
                return result;
            }
            currentRows = 0;
            return 0;
        }
        @Override public void insertBatch(Connection connection, long loadSessionId, List<CDEcomStageRow> rows) {
            insertCalls++;
            if (insertCalls == failOnInsertCall) throw new RuntimeException("stage failed");
            insertedExcelRows.add(rows.stream().map(CDEcomStageRow::excelRowNum).toList());
            currentRows += rows.size();
        }
    }

    private static final class FakeTarget extends CDEcomTargetRepository {
        final FakeStage stage;
        int publishCalls;
        Integer publishedRowsOverride;
        RuntimeException failure;
        FakeTarget(FakeStage stage) { this.stage = stage; }
        @Override public int publishFromStage(Connection connection, long loadSessionId) {
            publishCalls++;
            if (failure != null) throw failure;
            int result = publishedRowsOverride == null ? stage.currentRows : publishedRowsOverride;
            stage.publishCleanupPending = true;
            return result;
        }
    }

    private static final class FakeErrors extends CDEcomErrorRepository {
        final List<String> events;
        final List<List<Long>> insertedExcelRows = new ArrayList<>();
        final List<CDEcomValidationError> processingErrors = new ArrayList<>();
        int deleteCalls;
        boolean failChunkInsert;
        FakeErrors(List<String> events) { super(null); this.events = events; }
        @Override public void deleteByLoadSessionId(Connection connection, long loadSessionId) {
            events.add("deleteErrors");
            deleteCalls++;
        }
        @Override public void insertBatch(
                Connection connection, long loadSessionId, List<CDEcomValidationError> errors
        ) {
            if (failChunkInsert && !errors.isEmpty()) throw new RuntimeException("error insert failed");
            insertedExcelRows.add(errors.stream().map(CDEcomValidationError::excelRowNum).toList());
        }
        @Override public void insertAll(List<CDEcomValidationError> errors) {
            processingErrors.addAll(errors);
        }
    }

    private static DataSource dataSource(RecordingConnection recording) {
        return (DataSource) Proxy.newProxyInstance(
                DataSource.class.getClassLoader(), new Class<?>[]{DataSource.class},
                (proxy, method, args) -> "getConnection".equals(method.getName())
                        ? recording.proxy() : defaultValue(method.getReturnType())
        );
    }

    private static final class RecordingConnection {
        int commits;
        int rollbacks;
        int failCommitNumber;
        boolean autoCommit = true;
        Connection proxy() {
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        return switch (method.getName()) {
                            case "getAutoCommit" -> autoCommit;
                            case "setAutoCommit" -> {
                                autoCommit = (Boolean) args[0];
                                yield null;
                            }
                            case "commit" -> {
                                commits++;
                                if (commits == failCommitNumber) throw new SQLException("commit failed");
                                yield null;
                            }
                            case "rollback" -> {
                                rollbacks++;
                                yield null;
                            }
                            default -> defaultValue(method.getReturnType());
                        };
                    }
            );
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (type == void.class) return null;
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        throw new IllegalArgumentException(type.getName());
    }
}
