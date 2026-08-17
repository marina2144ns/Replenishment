package ru.stockmann.replenishment.services.salesbychannel.process;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Arrays;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesByChannelProcessorTest {

    @Test
    void blankNumericMetricsStageAsZeroAndPublish() {
        Transactions transactions = new Transactions();
        FakeStageRepository stage = new FakeStageRepository();
        FakeTargetRepository target = new FakeTargetRepository();
        target.publishedRows = 1;

        SalesByChannelProcessResult result = processor(
                true, transactions,
                new FakeRawRepository(List.of(List.of(blankMetricsRow(1)), List.of())),
                stage, new FakeErrorRepository(), target
        ).process(10L);

        assertTrue(result.success());
        SalesByChannelStageRow row = stage.insertedRows.get(0);
        assertEquals(0, row.salesQuantity());
        for (BigDecimal value : List.of(row.salesCurr(), row.gm(), row.discountTtl(), row.turnoverCurr())) {
            assertEquals(0, value.compareTo(BigDecimal.ZERO));
        }
    }

    @Test
    void invalidNumericMetricPersistsErrorAndPreventsTargetPublish() {
        Transactions transactions = new Transactions();
        FakeStageRepository stage = new FakeStageRepository();
        FakeErrorRepository errors = new FakeErrorRepository();
        FakeTargetRepository target = new FakeTargetRepository();

        SalesByChannelProcessResult result = processor(
                true, transactions,
                new FakeRawRepository(List.of(List.of(row(1, "2025", "April", "abc")), List.of())),
                stage, errors, target
        ).process(10L);

        assertFalse(result.success());
        assertEquals(1, result.errorRows());
        assertEquals(0, target.calls);
        assertTrue(stage.insertedRows.isEmpty());
    }

    @Test
    void processesMultipleChunksContinuesAfterErrorsAndNeverPublishesTarget() {
        Transactions transactions = new Transactions();
        FakeRawRepository raw = new FakeRawRepository(List.of(
                List.of(row(1, "2025", "April", "1"), row(4, null, "April", "2")),
                List.of(row(9, "FY", "Period", "3")),
                List.of()
        ));
        FakeStageRepository stage = new FakeStageRepository();
        FakeErrorRepository errors = new FakeErrorRepository();
        FakeTargetRepository target = new FakeTargetRepository();
        SalesByChannelProcessor processor = processor(
                true, transactions, raw, stage, errors, target
        );

        SalesByChannelProcessResult result = processor.process(10L);

        assertFalse(result.success());
        assertEquals(3, result.totalRows());
        assertEquals(2, result.stagedRows());
        assertEquals(0, result.loadedRows());
        assertEquals(1, result.errorRows());
        assertEquals(List.of(1L, 9L), stage.rawEquivalentIds);
        assertEquals(List.of(4L), errors.validationRawIds);
        assertEquals(List.of(0L, 4L, 9L), raw.lastIds);
        assertEquals(3, transactions.commits); // cleanup + two chunks
        assertEquals(1, stage.cleanupCalls);
        assertEquals(1, errors.cleanupCalls);
        assertEquals(0, target.calls);
    }

    @Test
    void repeatedRunStartsWithStageAndErrorCleanup() {
        Transactions transactions = new Transactions();
        FakeStageRepository stage = new FakeStageRepository();
        FakeErrorRepository errors = new FakeErrorRepository();
        FakeTargetRepository target = new FakeTargetRepository();
        target.publishedRows = 0;

        processor(true, transactions,
                new FakeRawRepository(List.of(List.of())), stage, errors,
                target).process(10L);
        processor(true, transactions,
                new FakeRawRepository(List.of(List.of())), stage, errors,
                target).process(10L);

        assertEquals(4, stage.cleanupCalls); // initial cleanup + publish cleanup for each run
        assertEquals(2, errors.cleanupCalls);
        assertEquals(2, target.calls);
        assertEquals(4, transactions.commits); // cleanup + empty publish for each run
        assertEquals(0, transactions.rollbacks);
        assertTrue(errors.bestEffort.isEmpty());
    }

    @Test
    void missingOrWrongLoadTypeChangesNothing() {
        Transactions transactions = new Transactions();
        FakeStageRepository stage = new FakeStageRepository();
        FakeErrorRepository errors = new FakeErrorRepository();
        FakeRawRepository raw = new FakeRawRepository(List.of(List.of(row(1, "2025", "April", "1"))));

        SalesByChannelProcessResult result =
                processor(false, transactions, raw, stage, errors, new FakeTargetRepository()).process(10L);

        assertFalse(result.success());
        assertEquals(0, result.totalRows());
        assertEquals(0, stage.cleanupCalls);
        assertEquals(0, errors.cleanupCalls);
        assertEquals(0, raw.calls);
        assertEquals(0, transactions.connections);
        assertTrue(errors.bestEffort.isEmpty());
    }

    @Test
    void technicalChunkFailureRollsBackThatChunkAndReportsProcessingError() {
        Transactions transactions = new Transactions();
        FakeStageRepository stage = new FakeStageRepository();
        stage.failInsert = true;
        FakeErrorRepository errors = new FakeErrorRepository();

        SalesByChannelProcessResult result = processor(
                true, transactions,
                new FakeRawRepository(List.of(List.of(row(1, "2025", "April", "1")))),
                stage, errors, new FakeTargetRepository()
        ).process(10L);

        assertFalse(result.success());
        assertEquals(1, transactions.commits); // cleanup only
        assertEquals(1, transactions.rollbacks);
        assertEquals(1, errors.bestEffort.size());
        assertEquals("PROCESSING", errors.bestEffort.get(0).errorLayer());
        assertEquals("UNEXPECTED_PROCESSING_ERROR", errors.bestEffort.get(0).errorCode());
    }

    @Test
    void validRowsPublishAndLoadedRowsEqualsActualInsertCount() {
        Transactions transactions = new Transactions();
        FakeTargetRepository target = new FakeTargetRepository();
        FakeStageRepository stage = new FakeStageRepository();

        SalesByChannelProcessResult result = processor(
                true, transactions,
                new FakeRawRepository(List.of(
                        List.of(row(1, "2025", "April", "1"), row(7, "2025", "May", "2")),
                        List.of()
                )),
                stage, new FakeErrorRepository(), target
        ).process(10L);

        assertTrue(result.success());
        assertEquals(2, result.totalRows());
        assertEquals(2, result.stagedRows());
        assertEquals(2, result.loadedRows());
        assertEquals(0, result.errorRows());
        assertEquals(1, target.calls);
        assertEquals(10L, target.loadSessionId);
        assertEquals(2, stage.cleanupCalls);
        assertEquals(2, stage.lastCleanedRows);
        assertEquals(3, transactions.commits); // cleanup, chunk, publish
    }

    @Test
    void emptyValidSessionPublishesSuccessfully() {
        Transactions transactions = new Transactions();
        FakeTargetRepository target = new FakeTargetRepository();
        target.publishedRows = 0;
        FakeErrorRepository errors = new FakeErrorRepository();
        FakeStageRepository stage = new FakeStageRepository();

        SalesByChannelProcessResult result = processor(
                true, transactions, new FakeRawRepository(List.of(List.of())),
                stage, errors, target
        ).process(10L);

        assertTrue(result.success());
        assertEquals(0, result.totalRows());
        assertEquals(0, result.stagedRows());
        assertEquals(0, result.loadedRows());
        assertEquals(0, result.errorRows());
        assertEquals(1, target.calls);
        assertEquals(10L, target.loadSessionId);
        assertEquals(2, stage.cleanupCalls); // initial cleanup + publish cleanup with expected zero
        assertEquals(0, stage.lastCleanedRows);
        assertTrue(errors.bestEffort.isEmpty());
        assertEquals(2, transactions.commits); // initial cleanup + empty publish
        assertEquals(0, transactions.rollbacks);
    }

    @Test
    void publishFailureRollsBackDeleteAndLeavesStageForRetry() {
        Transactions transactions = new Transactions();
        FakeStageRepository stage = new FakeStageRepository();
        FakeTargetRepository target = new FakeTargetRepository();
        target.fail = true;
        FakeErrorRepository errors = new FakeErrorRepository();

        SalesByChannelProcessResult result = processor(
                true, transactions,
                new FakeRawRepository(List.of(List.of(row(1, "2025", "April", "1")), List.of())),
                stage, errors, target
        ).process(10L);

        assertFalse(result.success());
        assertEquals(2, transactions.commits); // cleanup and chunk
        assertEquals(1, transactions.rollbacks); // complete publish transaction
        assertEquals(1, stage.cleanupCalls); // no cleanup after publish attempt
        assertEquals(1, errors.bestEffort.size());
    }

    @Test
    void stageCleanupCountMismatchRollsBackPublishAndReportsProcessingError() {
        Transactions transactions = new Transactions();
        FakeStageRepository stage = new FakeStageRepository();
        stage.cleanupCountOverride = 1;
        FakeErrorRepository errors = new FakeErrorRepository();

        SalesByChannelProcessResult result = processor(
                true, transactions,
                new FakeRawRepository(List.of(List.of(
                        row(1, "2025", "April", "1"),
                        row(2, "2025", "April", "2")
                ), List.of())),
                stage, errors, new FakeTargetRepository()
        ).process(10L);

        assertFalse(result.success());
        assertTrue(result.message().contains("stage cleanup row count mismatch"));
        assertEquals(2, stage.cleanupCalls);
        assertEquals(2, transactions.commits); // initial cleanup and chunk only
        assertEquals(1, transactions.rollbacks);
        assertEquals(1, errors.bestEffort.size());
    }

    @Test
    void stageCleanupFailureRollsBackPublishAndReportsProcessingError() {
        Transactions transactions = new Transactions();
        FakeStageRepository stage = new FakeStageRepository();
        stage.failOnCleanupCall = 2;
        FakeErrorRepository errors = new FakeErrorRepository();
        FakeTargetRepository target = new FakeTargetRepository();
        target.publishedRows = 1;

        SalesByChannelProcessResult result = processor(
                true, transactions,
                new FakeRawRepository(List.of(List.of(row(1, "2025", "April", "1")), List.of())),
                stage, errors, target
        ).process(10L);

        assertFalse(result.success());
        assertEquals(2, stage.cleanupCalls);
        assertEquals(2, transactions.commits); // initial cleanup and chunk only
        assertEquals(1, transactions.rollbacks);
        assertEquals(1, errors.bestEffort.size());
    }

    @Test
    void noSeparatePublicPublishEntryPointExists() {
        assertFalse(Arrays.stream(SalesByChannelProcessor.class.getMethods())
                .anyMatch(method -> method.getName().toLowerCase().contains("publish")));
        assertFalse(Arrays.stream(SalesByChannelTargetRepository.class.getMethods())
                .anyMatch(method -> method.getName().equals("publishFromStage")));
    }

    @Test
    void repeatedProcessRepublishesSameScopeWithoutAppendPath() {
        Transactions transactions = new Transactions();
        FakeTargetRepository target = new FakeTargetRepository();
        FakeStageRepository stage = new FakeStageRepository();
        FakeErrorRepository errors = new FakeErrorRepository();

        SalesByChannelProcessResult first = processor(
                true, transactions,
                new FakeRawRepository(List.of(List.of(
                        row(1, "2025", "April", "1"),
                        row(2, "2025", "May", "2")
                ), List.of())),
                stage, errors, target
        ).process(10L);
        SalesByChannelProcessResult second = processor(
                true, transactions,
                new FakeRawRepository(List.of(List.of(
                        row(1, "2025", "April", "1"),
                        row(2, "2025", "May", "2")
                ), List.of())),
                stage, errors, target
        ).process(10L);

        assertTrue(first.success());
        assertTrue(second.success());
        assertEquals(2, first.loadedRows());
        assertEquals(2, second.loadedRows());
        assertEquals(2, target.calls);
        assertEquals(4, stage.cleanupCalls);
        assertEquals(0, stage.stagedRows);
    }

    private SalesByChannelProcessor processor(
            boolean sessionExists,
            Transactions transactions,
            FakeRawRepository raw,
            FakeStageRepository stage,
            FakeErrorRepository errors,
            FakeTargetRepository target
    ) {
        return new SalesByChannelProcessor(
                transactions,
                new FakeSessionRepository(sessionExists),
                raw,
                stage,
                errors,
                target,
                new SalesByChannelValidator(),
                SalesByChannelProcessConfiguration.DEFAULT_CHUNK_SIZE
        );
    }

    private static final class FakeTargetRepository extends SalesByChannelTargetRepository {
        private int calls;
        private long loadSessionId;
        private boolean fail;
        private int publishedRows = 2;
        @Override int publishFromStage(Connection connection, long loadSessionId) {
            calls++;
            this.loadSessionId = loadSessionId;
            if (fail) throw new RuntimeException("target insert failed");
            return publishedRows;
        }
    }

    private SalesByChannelRawRow row(long id, String year, String month, String quantity) {
        return new SalesByChannelRawRow(
                id, 10L, id + 1, "sy", "s6", "ym", "ys", year, month,
                "channel", "store", "type", "division", "department", "campaign",
                "seasonality", "brand", quantity, "1", "2", "3", "4",
                "budget", "storeBpo", "channelBpo", "sub", "tm", "node", "section",
                "group", "phase", "product"
        );
    }

    private SalesByChannelRawRow blankMetricsRow(long id) {
        return new SalesByChannelRawRow(
                id, 10L, id + 1, "sy", "s6", "ym", "ys", "FY2025", "April",
                "channel", "store", "type", "division", "department", "campaign",
                "seasonality", "brand", null, "", " ", "N/A", "-",
                "budget", "storeBpo", "channelBpo", "sub", "tm", "node", "section",
                "group", "phase", "product"
        );
    }

    private static final class FakeSessionRepository extends SalesByChannelLoadSessionRepository {
        private final boolean exists;
        FakeSessionRepository(boolean exists) { super(null); this.exists = exists; }
        @Override public boolean existsById(long loadSessionId) { return exists; }
    }

    private static final class FakeRawRepository extends SalesByChannelRawRepository {
        private final Deque<List<SalesByChannelRawRow>> chunks;
        private final List<Long> lastIds = new ArrayList<>();
        private int calls;
        FakeRawRepository(List<List<SalesByChannelRawRow>> chunks) {
            super(null, 1_000);
            this.chunks = new ArrayDeque<>(chunks);
        }
        @Override public List<SalesByChannelRawRow> findChunk(long session, long lastId) {
            calls++;
            lastIds.add(lastId);
            return chunks.removeFirst();
        }
    }

    private static final class FakeStageRepository extends SalesByChannelStageRepository {
        private int cleanupCalls;
        private int stagedRows;
        private int lastCleanedRows;
        private int failOnCleanupCall = -1;
        private Integer cleanupCountOverride;
        private boolean failInsert;
        private final List<Long> rawEquivalentIds = new ArrayList<>();
        private final List<SalesByChannelStageRow> insertedRows = new ArrayList<>();
        @Override public int deleteByLoadSessionId(Connection connection, long id) {
            cleanupCalls++;
            if (cleanupCalls == failOnCleanupCall) {
                throw new RuntimeException("stage cleanup failed");
            }
            int cleanedRows = stagedRows;
            stagedRows = 0;
            lastCleanedRows = cleanedRows;
            if (cleanupCalls > 1 && cleanupCountOverride != null) {
                return cleanupCountOverride;
            }
            return cleanedRows;
        }
        @Override public void insertBatch(Connection connection, long id, List<SalesByChannelStageRow> rows) {
            if (failInsert) throw new RuntimeException("stage failed");
            stagedRows += rows.size();
            insertedRows.addAll(rows);
            rows.forEach(row -> rawEquivalentIds.add(row.excelRowNum() - 1));
        }
    }

    private static final class FakeErrorRepository extends SalesByChannelErrorRepository {
        private int cleanupCalls;
        private final List<Long> validationRawIds = new ArrayList<>();
        private final List<SalesByChannelValidationError> bestEffort = new ArrayList<>();
        FakeErrorRepository() { super(null); }
        @Override public void deleteByLoadSessionId(Connection connection, long id) { cleanupCalls++; }
        @Override public void insertBatch(Connection connection, long id,
                                         List<SalesByChannelValidationError> errors) {
            errors.forEach(error -> validationRawIds.add(error.rawId()));
        }
        @Override public void insertBestEffort(SalesByChannelValidationError error) {
            bestEffort.add(error);
        }
    }

    private static final class Transactions implements DataSource {
        private int connections;
        private int commits;
        private int rollbacks;
        @Override public Connection getConnection() {
            connections++;
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                        if ("getAutoCommit".equals(method.getName())) return true;
                        if ("commit".equals(method.getName())) { commits++; return null; }
                        if ("rollback".equals(method.getName())) { rollbacks++; return null; }
                        return defaultValue(method.getReturnType());
                    });
        }
        @Override public Connection getConnection(String u, String p) { return getConnection(); }
        @Override public <T> T unwrap(Class<T> iface) { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
        @Override public java.io.PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(java.io.PrintWriter out) { }
        @Override public void setLoginTimeout(int seconds) { }
        @Override public int getLoginTimeout() { return 0; }
        @Override public java.util.logging.Logger getParentLogger() { return null; }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }
}
