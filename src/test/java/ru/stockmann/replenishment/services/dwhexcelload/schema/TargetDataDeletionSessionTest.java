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
import ru.stockmann.replenishment.services.salesbychannel.process.SalesByChannelDeletionRepository;
import ru.stockmann.replenishment.services.salesbychannel.process.SalesByChannelDeletionService;

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
        assertNull(sessions.created.deleteMonth());
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
        assertNull(sessions.created.deleteMonth());
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

    @Test
    void cdDataCriteriaDeleteRecordsBothValuesAndExactCount() {
        Transaction transaction = new Transaction();
        RecordingSessionRepository sessions = new RecordingSessionRepository(9010L);
        CDDataDeletionRepository deletion = new CDDataDeletionRepository() {
            @Override public int deleteByNazvanieAndDen(Connection connection, String nazvanie, int den) {
                assertEquals("Main", nazvanie); assertEquals(15, den); return 2;
            }
        };

        DWHDataDeleteResult result = new CDDataDeletionService(
                transaction.dataSource(), deletion, sessions).deleteByNazvanieAndDen("Main", 15);

        assertEquals(2, result.deletedRows());
        assertCriteria(sessions.created, DWHExcelLoadType.CD_DATA,
                "NAZVANIE_DEN", "nazvanie", "Main", "den", "15");
        assertEquals(2, sessions.deletedRows);
        assertTrue(transaction.committed);
    }

    @Test
    void cdEcomCriteriaDeleteRecordsBothValuesAndZeroAsSuccess() {
        Transaction transaction = new Transaction();
        RecordingSessionRepository sessions = new RecordingSessionRepository(9011L);
        CDEcomDeletionRepository deletion = new CDEcomDeletionRepository() {
            @Override public int deleteByNazvanieAndDen(Connection connection, String nazvanie, int den) {
                return 0;
            }
        };

        DWHDataDeleteResult result = new CDEcomDeletionService(
                transaction.dataSource(), deletion, sessions).deleteByNazvanieAndDen("Online", 16);

        assertEquals(0, result.deletedRows());
        assertCriteria(sessions.created, DWHExcelLoadType.CD_ECOM,
                "NAZVANIE_DEN", "nazvanie", "Online", "den", "16");
        assertEquals(0, sessions.deletedRows);
        assertEquals(0, sessions.errorCalls);
    }

    @Test
    void salesYearMonthDeleteRecordsTypedAuditValuesAndExactCount() {
        Transaction transaction = new Transaction();
        RecordingSessionRepository sessions = new RecordingSessionRepository(9012L);
        SalesByChannelDeletionRepository deletion = new SalesByChannelDeletionRepository() {
            @Override public int deleteByYearAndMonth(Connection connection, String year, String month) {
                assertEquals("2026", year); assertEquals("08", month); return 4;
            }
        };

        DWHDataDeleteResult result = new SalesByChannelDeletionService(
                transaction.dataSource(), deletion, sessions).deleteByYearAndMonth("2026", "08");

        assertEquals(4, result.deletedRows());
        assertEquals(DWHExcelLoadType.SALES_BY_CHANNEL, sessions.created.loadType());
        assertEquals(DWHDeletionOperationMode.BY_PERIOD, sessions.created.operationMode());
        assertEquals(2026, sessions.created.deleteYear());
        assertEquals(8, sessions.created.deleteMonth());
        assertNull(sessions.created.deleteWeek());
        assertNull(sessions.created.sourceLoadSessionId());
        assertEquals(4, sessions.deletedRows);
        assertTrue(transaction.committed);
    }

    @Test
    void newCriteriaDeleteFailureRollsBackAndRecordsDiagnosticMessage() {
        Transaction transaction = new Transaction();
        RecordingSessionRepository sessions = new RecordingSessionRepository(9013L);
        CDEcomDeletionRepository deletion = new CDEcomDeletionRepository() {
            @Override public int deleteByNazvanieAndDen(Connection connection, String nazvanie, int den) {
                throw new RuntimeException("criteria delete failed");
            }
        };

        RuntimeException failure = assertThrows(RuntimeException.class, () ->
                new CDEcomDeletionService(transaction.dataSource(), deletion, sessions)
                        .deleteByNazvanieAndDen("Online", 16));

        assertEquals("criteria delete failed", failure.getMessage());
        assertTrue(transaction.rolledBack);
        assertFalse(transaction.committed);
        assertEquals(9013L, sessions.errorSessionId);
        assertEquals("criteria delete failed", sessions.errorMessage);
    }

    private static void assertCriteria(DWHDeletionSession session, DWHExcelLoadType loadType,
            String criterion, String name1, String value1, String name2, String value2) {
        assertEquals(loadType, session.loadType());
        assertEquals(DWHDeletionOperationMode.BY_CRITERIA, session.operationMode());
        assertEquals(criterion, session.deleteCriterion());
        assertEquals(name1, session.deleteParameter1Name());
        assertEquals(value1, session.deleteParameter1Value());
        assertEquals(name2, session.deleteParameter2Name());
        assertEquals(value2, session.deleteParameter2Value());
        assertNull(session.deleteYear());
        assertNull(session.deleteWeek());
        assertNull(session.deleteMonth());
        assertNull(session.sourceLoadSessionId());
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
