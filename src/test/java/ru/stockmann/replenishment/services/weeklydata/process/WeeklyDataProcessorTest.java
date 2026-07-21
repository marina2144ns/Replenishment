package ru.stockmann.replenishment.services.weeklydata.process;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyDataProcessorTest {

    private static final long LOAD_SESSION_ID = 10L;

    @Test
    void sessionNotFoundReturnsErrorWithoutReadingRawOrCleaningTables() {
        TestFixture fixture = new TestFixture();
        fixture.loadSessionRepository.exists = false;

        WeeklyDataProcessResult result = fixture.processor.process(LOAD_SESSION_ID);

        assertFalse(result.success());
        assertEquals(0, result.totalRows());
        assertEquals(0, result.loadedRows());
        assertEquals(1, result.errorRows());
        assertTrue(result.message().contains("loadSessionId=" + LOAD_SESSION_ID));
        assertTrue(result.message().contains("expected LoadTypeCode=WEEKLY_DATA"));

        assertEquals(0, fixture.rawRepository.findCalls);
        assertEquals(0, fixture.errorRepository.deleteCalls);
        assertEquals(0, fixture.targetRepository.deleteCalls);
    }

    @Test
    void wrongLoadTypeReturnsErrorWithoutReadingRawOrCleaningTables() {
        TestFixture fixture = new TestFixture();
        fixture.loadSessionRepository.exists = false;

        WeeklyDataProcessResult result = fixture.processor.process(LOAD_SESSION_ID);

        assertFalse(result.success());
        assertEquals(0, result.totalRows());
        assertEquals(0, result.loadedRows());
        assertEquals(1, result.errorRows());
        assertTrue(result.message().contains("expected LoadTypeCode=WEEKLY_DATA"));
        assertEquals(0, fixture.rawRepository.findCalls);
        assertEquals(0, fixture.errorRepository.deleteCalls);
        assertEquals(0, fixture.errorRepository.validationErrors.size());
        assertEquals(0, fixture.targetRepository.deleteCalls);
        assertEquals(0, fixture.targetRepository.insertedRows.size());
        assertEquals(0, fixture.dataSource.commitCount);
        assertEquals(0, fixture.dataSource.rollbackCount);
    }

    @Test
    void validationFailedWritesValidationErrorsAndDoesNotWriteTargetRows() {
        TestFixture fixture = new TestFixture();
        fixture.loadSessionRepository.exists = true;
        fixture.rawRepository.rows = List.of(row().year("").week("10").build());

        WeeklyDataProcessResult result = fixture.processor.process(LOAD_SESSION_ID);

        assertFalse(result.success());
        assertEquals(1, result.totalRows());
        assertEquals(0, result.loadedRows());
        assertTrue(result.errorRows() > 0);

        assertEquals(1, fixture.errorRepository.deleteCalls);
        assertEquals(1, fixture.targetRepository.deleteCalls);
        assertEquals(1, fixture.errorRepository.validationErrors.size());
        assertEquals(0, fixture.targetRepository.insertedRows.size());
        assertEquals(1, fixture.dataSource.commitCount);
        assertEquals(0, fixture.dataSource.rollbackCount);
    }

    @Test
    void textLengthValidationFailureWritesErrorAndBlocksTargetInsert() {
        TestFixture fixture = new TestFixture();
        fixture.loadSessionRepository.exists = true;
        fixture.rawRepository.rows = List.of(row()
                .year("2025")
                .week("10")
                .storeRus("a".repeat(256))
                .build());

        WeeklyDataProcessResult result = fixture.processor.process(LOAD_SESSION_ID);

        assertFalse(result.success());
        assertEquals(1, result.totalRows());
        assertEquals(0, result.loadedRows());
        assertEquals(1, result.errorRows());
        assertEquals(1, fixture.errorRepository.validationErrors.size());
        assertEquals("StoreRus", fixture.errorRepository.validationErrors.get(0).fieldName());
        assertEquals("TEXT_TOO_LONG", fixture.errorRepository.validationErrors.get(0).errorCode());
        assertEquals(3L, fixture.errorRepository.validationErrors.get(0).excelRowNum());
        assertEquals(0, fixture.targetRepository.insertedRows.size());
        assertEquals(1, fixture.dataSource.commitCount);
        assertEquals(0, fixture.dataSource.rollbackCount);
    }

    @Test
    void successWritesTargetRowsAndDoesNotWriteValidationErrors() {
        TestFixture fixture = new TestFixture();
        fixture.loadSessionRepository.exists = true;
        fixture.rawRepository.rows = List.of(row().year("2025").week("10").build());

        WeeklyDataProcessResult result = fixture.processor.process(LOAD_SESSION_ID);

        assertTrue(result.success());
        assertEquals(1, result.totalRows());
        assertEquals(1, result.loadedRows());
        assertEquals(0, result.errorRows());

        assertEquals(1, fixture.errorRepository.deleteCalls);
        assertEquals(1, fixture.targetRepository.deleteCalls);
        assertEquals(0, fixture.errorRepository.validationErrors.size());
        assertEquals(1, fixture.targetRepository.insertedRows.size());
        assertEquals(1, fixture.dataSource.commitCount);
        assertEquals(0, fixture.dataSource.rollbackCount);
    }

    @Test
    void unexpectedExceptionWritesProcessingErrorOutsideTransaction() {
        TestFixture fixture = new TestFixture();
        fixture.loadSessionRepository.exists = true;
        fixture.rawRepository.rows = List.of(row().year("2025").week("10").build());
        fixture.targetRepository.throwOnInsert = true;

        WeeklyDataProcessResult result = fixture.processor.process(LOAD_SESSION_ID);

        assertFalse(result.success());
        assertEquals(1, result.totalRows());
        assertEquals(0, result.loadedRows());
        assertEquals(1, result.errorRows());

        assertEquals(0, fixture.dataSource.commitCount);
        assertEquals(1, fixture.dataSource.rollbackCount);
        assertEquals(1, fixture.errorRepository.processingErrors.size());
        assertEquals("PROCESSING", fixture.errorRepository.processingErrors.get(0).errorLayer());
        assertEquals("UNEXPECTED_PROCESSING_ERROR", fixture.errorRepository.processingErrors.get(0).errorCode());
    }

    private static RowBuilder row() {
        return new RowBuilder();
    }

    private static class TestFixture {
        private final RecordingDataSource dataSource = new RecordingDataSource();
        private final FakeLoadSessionRepository loadSessionRepository = new FakeLoadSessionRepository();
        private final FakeRawRepository rawRepository = new FakeRawRepository();
        private final FakeErrorRepository errorRepository = new FakeErrorRepository();
        private final FakeTargetRepository targetRepository = new FakeTargetRepository();
        private final WeeklyDataProcessor processor = new WeeklyDataProcessor(
                dataSource,
                loadSessionRepository,
                rawRepository,
                errorRepository,
                targetRepository,
                new WeeklyDataValidator(),
                new WeeklyDataRowMapper()
        );
    }

    private static class FakeLoadSessionRepository extends WeeklyDataLoadSessionRepository {
        private boolean exists;

        FakeLoadSessionRepository() {
            super(null);
        }

        @Override
        public boolean existsById(long loadSessionId) {
            return exists;
        }
    }

    private static class FakeRawRepository extends WeeklyDataRawRepository {
        private List<WeeklyDataRawRow> rows = List.of();
        private int findCalls;

        FakeRawRepository() {
            super(null);
        }

        @Override
        public List<WeeklyDataRawRow> findByLoadSessionId(Connection connection, long loadSessionId) {
            findCalls++;
            return rows;
        }
    }

    private static class FakeErrorRepository extends WeeklyDataErrorRepository {
        private int deleteCalls;
        private final List<WeeklyDataValidationError> validationErrors = new ArrayList<>();
        private final List<WeeklyDataValidationError> processingErrors = new ArrayList<>();

        FakeErrorRepository() {
            super(null);
        }

        @Override
        public void deleteByLoadSessionId(Connection connection, long loadSessionId) {
            deleteCalls++;
        }

        @Override
        public void insertAll(Connection connection, List<WeeklyDataValidationError> errors) {
            validationErrors.addAll(errors);
        }

        @Override
        public void insertAll(List<WeeklyDataValidationError> errors) {
            processingErrors.addAll(errors);
        }
    }

    private static class FakeTargetRepository extends WeeklyDataTargetRepository {
        private int deleteCalls;
        private boolean throwOnInsert;
        private final List<WeeklyDataTargetRow> insertedRows = new ArrayList<>();

        FakeTargetRepository() {
            super(null);
        }

        @Override
        public void deleteByLoadSessionId(Connection connection, long loadSessionId) {
            deleteCalls++;
        }

        @Override
        public void insertAll(Connection connection, List<WeeklyDataTargetRow> rows) {
            if (throwOnInsert) {
                throw new RuntimeException("target insert failed");
            }
            insertedRows.addAll(rows);
        }
    }

    private static class RecordingDataSource implements DataSource {
        private int commitCount;
        private int rollbackCount;

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

        private Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) {
                return null;
            }
            if (type == boolean.class) {
                return false;
            }
            if (type == void.class) {
                return null;
            }
            return 0;
        }
    }

    private static class RowBuilder {
        private String year;
        private String week;
        private String storeRus;

        RowBuilder year(String year) {
            this.year = year;
            return this;
        }

        RowBuilder week(String week) {
            this.week = week;
            return this;
        }

        RowBuilder storeRus(String storeRus) {
            this.storeRus = storeRus;
            return this;
        }

        WeeklyDataRawRow build() {
            return new WeeklyDataRawRow(
                    1,
                    2,
                    3L,
                    null,
                    null,
                    null,
                    null,
                    year,
                    week,
                    null,
                    null,
                    storeRus,
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
    }
}
