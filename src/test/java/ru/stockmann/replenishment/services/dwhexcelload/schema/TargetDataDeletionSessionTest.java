package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.cddata.process.CDDataDeletionRepository;
import ru.stockmann.replenishment.services.cddata.process.CDDataDeletionService;
import ru.stockmann.replenishment.services.cdecom.process.CDEcomDeletionRepository;
import ru.stockmann.replenishment.services.cdecom.process.CDEcomDeletionService;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDataDeleteResult;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDeletionOperationMode;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDeletionSession;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDeletionSessionRepository;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadType;
import ru.stockmann.replenishment.services.weeklydata.process.WeeklyDataDeletionRepository;
import ru.stockmann.replenishment.services.weeklydata.process.WeeklyDataDeletionService;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetDataDeletionSessionTest {

    @Test
    void weeklyPeriodDeleteCreatesIndependentSessionWithParametersAndCount() {
        Transaction transaction = new Transaction();
        RecordingSessionRepository sessions = new RecordingSessionRepository(9001L);
        WeeklyDataDeletionRepository deletion = new WeeklyDataDeletionRepository() {
            @Override
            public int deleteByPeriod(Connection connection, short year, short week) {
                return 1250;
            }
        };

        DWHDataDeleteResult result = new WeeklyDataDeletionService(
                transaction.dataSource(), deletion, sessions
        ).deleteByPeriod((short) 2026, (short) 31);

        assertEquals(1250, result.deletedRows());
        assertEquals(DWHExcelLoadType.WEEKLY_DATA, sessions.created.loadType());
        assertEquals(DWHDeletionOperationMode.BY_PERIOD, sessions.created.operationMode());
        assertEquals(2026, sessions.created.deleteYear());
        assertEquals(31, sessions.created.deleteWeek());
        assertNull(sessions.created.sourceLoadSessionId());
        assertEquals(9001L, sessions.completedSessionId);
        assertEquals(1250, sessions.deletedRows);
        assertTrue(transaction.committed);
        assertFalse(transaction.rolledBack);
    }

    @Test
    void cdDataLoadSessionDeleteKeepsSourceIdSeparateFromDeletionSession() {
        long sourceLoadSessionId = 10521L;
        long deletionSessionId = 9002L;
        Transaction transaction = new Transaction();
        RecordingSessionRepository sessions =
                new RecordingSessionRepository(deletionSessionId);
        CDDataDeletionRepository deletion = new CDDataDeletionRepository() {
            @Override
            public int deleteByLoadSessionId(Connection connection, long loadSessionId) {
                assertEquals(sourceLoadSessionId, loadSessionId);
                return 840;
            }
        };

        new CDDataDeletionService(transaction.dataSource(), deletion, sessions)
                .deleteByLoadSessionId(sourceLoadSessionId);

        assertEquals(DWHExcelLoadType.CD_DATA, sessions.created.loadType());
        assertEquals(DWHDeletionOperationMode.BY_LOAD_SESSION, sessions.created.operationMode());
        assertNull(sessions.created.deleteYear());
        assertNull(sessions.created.deleteWeek());
        assertEquals(sourceLoadSessionId, sessions.created.sourceLoadSessionId());
        assertEquals(deletionSessionId, sessions.completedSessionId);
        assertTrue(deletionSessionId != sourceLoadSessionId);
        assertEquals(840, sessions.deletedRows);
    }

    @Test
    void cdecomZeroDeleteIsRecordedAsSuccessful() {
        Transaction transaction = new Transaction();
        RecordingSessionRepository sessions = new RecordingSessionRepository(9003L);
        CDEcomDeletionRepository deletion = new CDEcomDeletionRepository() {
            @Override
            public int deleteByPeriod(Connection connection, int year, int week) {
                return 0;
            }
        };

        DWHDataDeleteResult result = new CDEcomDeletionService(
                transaction.dataSource(), deletion, sessions
        ).deleteByPeriod(2026, 31);

        assertEquals(0, result.deletedRows());
        assertEquals(DWHExcelLoadType.CD_ECOM, sessions.created.loadType());
        assertEquals(0, sessions.deletedRows);
        assertEquals(0, sessions.errorCalls);
        assertTrue(transaction.committed);
    }

    @Test
    void failedDeleteRollsBackAndMarksNewSessionAsError() {
        Transaction transaction = new Transaction();
        RecordingSessionRepository sessions = new RecordingSessionRepository(9004L);
        WeeklyDataDeletionRepository deletion = new WeeklyDataDeletionRepository() {
            @Override
            public int deleteByPeriod(Connection connection, short year, short week) {
                throw new RuntimeException("delete failed");
            }
        };

        RuntimeException failure = assertThrows(RuntimeException.class, () ->
                new WeeklyDataDeletionService(transaction.dataSource(), deletion, sessions)
                        .deleteByPeriod((short) 2026, (short) 31)
        );

        assertEquals("delete failed", failure.getMessage());
        assertTrue(transaction.rolledBack);
        assertFalse(transaction.committed);
        assertEquals(9004L, sessions.errorSessionId);
        assertEquals("delete failed", sessions.errorMessage);
        assertEquals(1, sessions.errorCalls);
    }

    private static final class RecordingSessionRepository
            extends DWHDeletionSessionRepository {
        private final long newSessionId;
        private DWHDeletionSession created;
        private long completedSessionId;
        private long deletedRows = -1;
        private long errorSessionId;
        private String errorMessage;
        private int errorCalls;

        private RecordingSessionRepository(long newSessionId) {
            super(null);
            this.newSessionId = newSessionId;
        }

        @Override
        public long create(DWHDeletionSession session) {
            created = session;
            return newSessionId;
        }

        @Override
        public void completeSuccess(Connection connection, long sessionId, long deletedRows) {
            completedSessionId = sessionId;
            this.deletedRows = deletedRows;
        }

        @Override
        public void completeError(long sessionId, String message) {
            errorCalls++;
            errorSessionId = sessionId;
            errorMessage = message;
        }
    }

    private static final class Transaction {
        private boolean autoCommit = true;
        private boolean committed;
        private boolean rolledBack;

        private DataSource dataSource() {
            Connection connection = (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getAutoCommit" -> autoCommit;
                        case "setAutoCommit" -> {
                            autoCommit = (Boolean) args[0];
                            yield null;
                        }
                        case "commit" -> {
                            committed = true;
                            yield null;
                        }
                        case "rollback" -> {
                            rolledBack = true;
                            yield null;
                        }
                        case "close" -> null;
                        default -> defaultValue(method.getReturnType());
                    }
            );
            return (DataSource) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{DataSource.class},
                    (proxy, method, args) ->
                            "getConnection".equals(method.getName())
                                    ? connection
                                    : defaultValue(method.getReturnType())
            );
        }

        private static Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) return null;
            if (type == boolean.class) return false;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            return null;
        }
    }
}
