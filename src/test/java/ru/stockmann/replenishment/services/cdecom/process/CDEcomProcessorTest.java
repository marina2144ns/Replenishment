package ru.stockmann.replenishment.services.cdecom.process;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDEcomProcessorTest {

    @Test
    void missingLoadSessionReturnsFailureWithoutOpeningProcessingTransaction() {
        TestContext context = TestContext.withRows(row(1));
        context.loadSessionRepository.exists = false;

        CDEcomProcessResult result = context.processor().process(100L);

        assertEquals(new CDEcomProcessResult(
                100L,
                false,
                0,
                0,
                1,
                "Load session not found or has unexpected LoadTypeCode. loadSessionId=100, expected LoadTypeCode=CD_ECOM"
        ), result);
        assertEquals(1, context.loadSessionRepository.existsCalls);
        assertFalse(context.connection.commitCalled);
        assertFalse(context.connection.rollbackCalled);
        assertTrue(context.connection.setAutoCommitValues.isEmpty());
        assertTrue(context.validator.validatedRows.isEmpty());
        assertTrue(context.mapper.mappedRows.isEmpty());
        assertEquals(0, context.errorRepository.insertAllInTransactionCalls);
    }

    @Test
    void wrongLoadTypeReturnsFailureWithoutOpeningProcessingTransaction() {
        TestContext context = TestContext.withRows(row(1));
        context.loadSessionRepository.exists = false;

        CDEcomProcessResult result = context.processor().process(100L);

        assertEquals(new CDEcomProcessResult(
                100L,
                false,
                0,
                0,
                1,
                "Load session not found or has unexpected LoadTypeCode. loadSessionId=100, expected LoadTypeCode=CD_ECOM"
        ), result);
        assertEquals(1, context.loadSessionRepository.existsCalls);
        assertFalse(context.connection.commitCalled);
        assertFalse(context.connection.rollbackCalled);
        assertTrue(context.connection.setAutoCommitValues.isEmpty());
        assertTrue(context.validator.validatedRows.isEmpty());
        assertTrue(context.mapper.mappedRows.isEmpty());
        assertEquals(0, context.errorRepository.insertAllInTransactionCalls);
        assertTrue(context.errorRepository.processingErrors.isEmpty());
        assertEquals(List.of("exists"), context.events);
    }

    @Test
    void successfulSessionCommitsAndInsertsTargets() {
        TestContext context = TestContext.withRows(row(1), row(2));

        CDEcomProcessResult result = context.processor().process(100L);

        assertEquals(new CDEcomProcessResult(
                100L,
                true,
                2,
                2,
                0,
                "CDEcom load session processed successfully"
        ), result);
        assertSame(context.connection.proxy(), context.rawRepository.connection);
        assertEquals(List.of(row(1), row(2)), context.validator.validatedRows);
        assertEquals(List.of(row(1), row(2)), context.mapper.mappedRows);
        assertEquals(1, context.targetRepository.insertAllCalls);
        assertEquals(0, context.errorRepository.insertAllInTransactionCalls);
        assertTrue(context.connection.commitCalled);
        assertFalse(context.connection.rollbackCalled);
        assertEquals(List.of(
                "exists",
                "setAutoCommit:false",
                "readRaw",
                "deleteErrors",
                "deleteTarget",
                "validate:1",
                "validate:2",
                "map:1",
                "map:2",
                "insertTarget",
                "commit",
                "setAutoCommit:true"
        ), context.events);
    }

    @Test
    void validationErrorsCommitErrorsWithoutMappingOrTargetInsert() {
        CDEcomRawRow row1 = row(1);
        CDEcomRawRow row2 = row(2);
        TestContext context = TestContext.withRows(row1, row2);
        context.validator.errorsByRawId.put(1L, List.of(error(row1, "god"), error(row1, "data")));
        context.validator.errorsByRawId.put(2L, List.of(error(row2, "planRub")));

        CDEcomProcessResult result = context.processor().process(100L);

        assertEquals(100L, result.loadSessionId());
        assertFalse(result.success());
        assertEquals(2, result.totalRows());
        assertEquals(0, result.loadedRows());
        assertEquals(3, result.errorRows());
        assertEquals("Validation failed", result.message());
        assertEquals(List.of(row1, row2), context.validator.validatedRows);
        assertTrue(context.mapper.mappedRows.isEmpty());
        assertEquals(0, context.targetRepository.insertAllCalls);
        assertEquals(1, context.errorRepository.insertAllInTransactionCalls);
        assertEquals(3, context.errorRepository.transactionErrors.size());
        assertTrue(context.connection.commitCalled);
        assertFalse(context.connection.rollbackCalled);
    }

    @Test
    void multipleErrorsInOneRawRowInsertAllErrorRecordsAndCountAllErrors() {
        CDEcomRawRow row = row(1);
        TestContext context = TestContext.withRows(row);
        context.validator.errorsByRawId.put(1L, List.of(
                error(row, "god"),
                error(row, "data"),
                error(row, "planRub")
        ));

        CDEcomProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals(3, result.errorRows());
        assertEquals(3, context.errorRepository.transactionErrors.size());
    }

    @Test
    void emptyRawSessionDeletesOldDataCommitsAndReturnsSuccess() {
        TestContext context = TestContext.withRows();

        CDEcomProcessResult result = context.processor().process(100L);

        assertEquals(new CDEcomProcessResult(
                100L,
                true,
                0,
                0,
                0,
                "CDEcom load session processed successfully"
        ), result);
        assertEquals(0, context.errorRepository.insertAllInTransactionCalls);
        assertEquals(0, context.targetRepository.insertAllCalls);
        assertTrue(context.connection.commitCalled);
        assertEquals(List.of(
                "exists",
                "setAutoCommit:false",
                "readRaw",
                "deleteErrors",
                "deleteTarget",
                "commit",
                "setAutoCommit:true"
        ), context.events);
    }

    @Test
    void rawReadExceptionRollsBackWritesProcessingErrorAndReturnsFailure() {
        TestContext context = TestContext.withRows(row(1));
        context.rawRepository.failure = new RuntimeException("raw failed");

        CDEcomProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals(0, result.totalRows());
        assertEquals(0, result.loadedRows());
        assertEquals(1, result.errorRows());
        assertEquals("raw failed", result.message());
        assertTrue(context.connection.rollbackCalled);
        assertFalse(context.connection.commitCalled);
        assertProcessingError(context, "raw failed");
    }

    @Test
    void deleteExceptionRollsBackWritesProcessingErrorAndReturnsFailure() {
        TestContext context = TestContext.withRows(row(1));
        context.errorRepository.deleteFailure = new RuntimeException("delete failed");

        CDEcomProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals(1, result.totalRows());
        assertEquals(1, result.errorRows());
        assertTrue(context.connection.rollbackCalled);
        assertFalse(context.connection.commitCalled);
        assertProcessingError(context, "delete failed");
    }

    @Test
    void errorInsertExceptionRollsBackWritesProcessingErrorAndReturnsFailure() {
        CDEcomRawRow row = row(1);
        TestContext context = TestContext.withRows(row);
        context.validator.errorsByRawId.put(1L, List.of(error(row, "god")));
        context.errorRepository.transactionInsertFailure = new RuntimeException("error insert failed");

        CDEcomProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals(1, result.totalRows());
        assertEquals(1, result.errorRows());
        assertTrue(context.connection.rollbackCalled);
        assertFalse(context.connection.commitCalled);
        assertProcessingError(context, "error insert failed");
    }

    @Test
    void targetInsertExceptionRollsBackWritesProcessingErrorAndReturnsFailure() {
        TestContext context = TestContext.withRows(row(1));
        context.targetRepository.insertFailure = new RuntimeException("target insert failed");

        CDEcomProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals(1, result.totalRows());
        assertEquals(1, result.errorRows());
        assertTrue(context.connection.rollbackCalled);
        assertFalse(context.connection.commitCalled);
        assertProcessingError(context, "target insert failed");
    }

    @Test
    void restoresOriginalAutoCommit() {
        TestContext context = TestContext.withRows(row(1));

        context.processor().process(100L);

        assertEquals(List.of(false, true), context.connection.setAutoCommitValues);
    }

    @Test
    void processingErrorInsertFailureIsSuppressed() {
        TestContext context = TestContext.withRows(row(1));
        context.rawRepository.failure = new RuntimeException("raw failed");
        context.errorRepository.processingInsertFailure = new RuntimeException("processing insert failed");

        CDEcomProcessResult result = context.processor().process(100L);

        assertFalse(result.success());
        assertEquals("raw failed", result.message());
        assertTrue(context.connection.rollbackCalled);
    }

    private static void assertProcessingError(TestContext context, String message) {
        assertEquals(1, context.errorRepository.processingErrors.size());
        CDEcomValidationError error = context.errorRepository.processingErrors.get(0);
        assertEquals(100L, error.loadSessionId());
        assertEquals(0L, error.rawId());
        assertEquals("PROCESSING", error.errorLayer());
        assertEquals("UNEXPECTED_PROCESSING_ERROR", error.errorCode());
        assertEquals(message, error.errorReason());
        assertEquals("Unexpected processing error: " + message, error.errorMessage());
    }

    private static CDEcomRawRow row(long id) {
        return new CDEcomRawRow(
                id,
                100L,
                id + 1,
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
                null,
                null
        );
    }

    private static CDEcomTargetRow targetRow(long id) {
        return new CDEcomTargetRow(
                100L,
                "target-" + id,
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

    private static CDEcomValidationError error(CDEcomRawRow row, String fieldName) {
        return new CDEcomValidationError(
                row.loadSessionId(),
                row.id(),
                row.excelRowNum(),
                "VALIDATION",
                fieldName,
                "INVALID_VALUE",
                "reason",
                "message"
        );
    }

    private static final class TestContext {
        private final List<String> events = new ArrayList<>();
        private final RecordingConnection connection = new RecordingConnection(events);
        private final FakeLoadSessionRepository loadSessionRepository;
        private final FakeRawRepository rawRepository;
        private final FakeTargetRepository targetRepository;
        private final FakeErrorRepository errorRepository;
        private final FakeValidator validator;
        private final FakeMapper mapper;

        private TestContext(List<CDEcomRawRow> rows) {
            this.loadSessionRepository = new FakeLoadSessionRepository(events);
            this.rawRepository = new FakeRawRepository(events, rows);
            this.targetRepository = new FakeTargetRepository(events);
            this.errorRepository = new FakeErrorRepository(events);
            this.validator = new FakeValidator(events);
            this.mapper = new FakeMapper(events);
        }

        static TestContext withRows(CDEcomRawRow... rows) {
            return new TestContext(List.of(rows));
        }

        CDEcomProcessor processor() {
            return new CDEcomProcessor(
                    dataSource(connection),
                    loadSessionRepository,
                    rawRepository,
                    targetRepository,
                    errorRepository,
                    validator,
                    mapper
            );
        }
    }

    private static final class FakeLoadSessionRepository extends CDEcomLoadSessionRepository {
        private final List<String> events;
        private boolean exists = true;
        private int existsCalls;

        private FakeLoadSessionRepository(List<String> events) {
            super(null);
            this.events = events;
        }

        @Override
        public boolean existsById(long loadSessionId) {
            events.add("exists");
            existsCalls++;
            return exists;
        }
    }

    private static DataSource dataSource(RecordingConnection connection) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("getConnection".equals(method.getName())) {
                return connection.proxy();
            }
            return defaultValue(method.getReturnType());
        };
        return (DataSource) Proxy.newProxyInstance(
                DataSource.class.getClassLoader(),
                new Class<?>[]{DataSource.class},
                handler
        );
    }

    private static final class RecordingConnection {
        private final List<String> events;
        private final List<Boolean> setAutoCommitValues = new ArrayList<>();
        private boolean autoCommit = true;
        private boolean commitCalled;
        private boolean rollbackCalled;
        private Connection proxy;

        private RecordingConnection(List<String> events) {
            this.events = events;
        }

        Connection proxy() {
            if (proxy == null) {
                InvocationHandler handler = (p, method, args) -> {
                    String name = method.getName();
                    if ("getAutoCommit".equals(name)) {
                        return autoCommit;
                    }
                    if ("setAutoCommit".equals(name)) {
                        autoCommit = (Boolean) args[0];
                        setAutoCommitValues.add(autoCommit);
                        events.add("setAutoCommit:" + autoCommit);
                        return null;
                    }
                    if ("commit".equals(name)) {
                        commitCalled = true;
                        events.add("commit");
                        return null;
                    }
                    if ("rollback".equals(name)) {
                        rollbackCalled = true;
                        events.add("rollback");
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                };
                proxy = (Connection) Proxy.newProxyInstance(
                        Connection.class.getClassLoader(),
                        new Class<?>[]{Connection.class},
                        handler
                );
            }
            return proxy;
        }
    }

    private static final class FakeRawRepository extends CDEcomRawRepository {
        private final List<String> events;
        private final List<CDEcomRawRow> rows;
        private RuntimeException failure;
        private Connection connection;

        private FakeRawRepository(List<String> events, List<CDEcomRawRow> rows) {
            super(null);
            this.events = events;
            this.rows = rows;
        }

        @Override
        public List<CDEcomRawRow> findByLoadSessionId(Connection connection, long loadSessionId) {
            events.add("readRaw");
            this.connection = connection;
            if (failure != null) {
                throw failure;
            }
            return rows;
        }
    }

    private static final class FakeTargetRepository extends CDEcomTargetRepository {
        private final List<String> events;
        private int insertAllCalls;
        private RuntimeException insertFailure;

        private FakeTargetRepository(List<String> events) {
            super(null);
            this.events = events;
        }

        @Override
        public void deleteByLoadSessionId(Connection connection, long loadSessionId) {
            events.add("deleteTarget");
        }

        @Override
        public void insertAll(Connection connection, List<CDEcomTargetRow> rows) {
            events.add("insertTarget");
            insertAllCalls++;
            if (insertFailure != null) {
                throw insertFailure;
            }
        }
    }

    private static final class FakeErrorRepository extends CDEcomErrorRepository {
        private final List<String> events;
        private int insertAllInTransactionCalls;
        private RuntimeException deleteFailure;
        private RuntimeException transactionInsertFailure;
        private RuntimeException processingInsertFailure;
        private final List<CDEcomValidationError> transactionErrors = new ArrayList<>();
        private final List<CDEcomValidationError> processingErrors = new ArrayList<>();

        private FakeErrorRepository(List<String> events) {
            super(null);
            this.events = events;
        }

        @Override
        public void deleteByLoadSessionId(Connection connection, long loadSessionId) {
            events.add("deleteErrors");
            if (deleteFailure != null) {
                throw deleteFailure;
            }
        }

        @Override
        public void insertAll(Connection connection, List<CDEcomValidationError> errors) {
            events.add("insertErrors");
            insertAllInTransactionCalls++;
            if (transactionInsertFailure != null) {
                throw transactionInsertFailure;
            }
            transactionErrors.addAll(errors);
        }

        @Override
        public void insertAll(List<CDEcomValidationError> errors) {
            events.add("insertProcessingError");
            if (processingInsertFailure != null) {
                throw processingInsertFailure;
            }
            processingErrors.addAll(errors);
        }
    }

    private static final class FakeValidator extends CDEcomValidator {
        private final List<String> events;
        private final List<CDEcomRawRow> validatedRows = new ArrayList<>();
        private final java.util.Map<Long, List<CDEcomValidationError>> errorsByRawId = new java.util.HashMap<>();

        private FakeValidator(List<String> events) {
            this.events = events;
        }

        @Override
        public CDEcomValidationResult validate(CDEcomRawRow row) {
            events.add("validate:" + row.id());
            validatedRows.add(row);
            return new CDEcomValidationResult(
                    row,
                    errorsByRawId.getOrDefault(row.id(), List.of())
            );
        }
    }

    private static final class FakeMapper extends CDEcomRowMapper {
        private final List<String> events;
        private final List<CDEcomRawRow> mappedRows = new ArrayList<>();

        private FakeMapper(List<String> events) {
            this.events = events;
        }

        @Override
        public CDEcomTargetRow toTargetRow(CDEcomRawRow row) {
            events.add("map:" + row.id());
            mappedRows.add(row);
            return targetRow(row.id());
        }
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == Void.TYPE) {
            return null;
        }
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Byte.TYPE) {
            return (byte) 0;
        }
        if (returnType == Short.TYPE) {
            return (short) 0;
        }
        if (returnType == Integer.TYPE) {
            return 0;
        }
        if (returnType == Long.TYPE) {
            return 0L;
        }
        if (returnType == Float.TYPE) {
            return 0F;
        }
        if (returnType == Double.TYPE) {
            return 0D;
        }
        if (returnType == Character.TYPE) {
            return '\0';
        }
        return null;
    }
}
