package ru.stockmann.replenishment.services.salesbychannel.process;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDataDeleteResult;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDeletionOperationMode;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDeletionSession;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDeletionSessionRepository;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadType;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesByChannelDeletionTest {

    @Test
    void repositoryDeletesOnlyTargetPeriodWithParameters() {
        Jdbc jdbc = new Jdbc(12);

        int deleted = new SalesByChannelDeletionRepository()
                .deleteByYearAndMonth(jdbc.connection(), "2026", "7");

        assertEquals(12, deleted);
        assertTrue(jdbc.sql.contains("DELETE FROM dbo.SalesByChannel"));
        assertTrue(jdbc.sql.contains("[year] = ?"));
        assertTrue(jdbc.sql.contains("[month] = ?"));
        assertFalse(jdbc.sql.contains("SalesByChannel_raw"));
        assertFalse(jdbc.sql.contains("SalesByChannel_stage"));
        assertFalse(jdbc.sql.contains("DWH_Excel_Load_Error"));
        assertEquals(List.of("setString:1:2026", "setString:2:7"), jdbc.calls);
    }

    @Test
    void repositoryDeletesOnlyRequestedTargetLoadSession() {
        Jdbc jdbc = new Jdbc(7);

        int deleted = new SalesByChannelDeletionRepository()
                .deleteByLoadSessionId(jdbc.connection(), 10521L);

        assertEquals(7, deleted);
        assertTrue(jdbc.sql.contains("WHERE LoadSessionId = ?"));
        assertFalse(jdbc.sql.contains("DWH_Excel_Load_Session"));
        assertEquals(List.of("setLong:1:10521"), jdbc.calls);
    }

    @Test
    void textualYearMonthDeletionCreatesLosslessAuditSessionWithExactCount() {
        Transaction transaction = new Transaction();
        Sessions sessions = new Sessions(9001L);
        SalesByChannelDeletionRepository deletion = new SalesByChannelDeletionRepository() {
            @Override
            public int deleteByYearAndMonth(Connection connection, String year, String month) {
                assertEquals("FY2025", year);
                assertEquals("April", month);
                return 1250;
            }
        };

        DWHDataDeleteResult result = new SalesByChannelDeletionService(
                transaction.dataSource(), deletion, sessions
        ).deleteByYearAndMonth("FY2025", "April");

        assertEquals(1250, result.deletedRows());
        assertEquals(DWHExcelLoadType.SALES_BY_CHANNEL, sessions.created.loadType());
        assertEquals(DWHDeletionOperationMode.BY_PERIOD, sessions.created.operationMode());
        assertNull(sessions.created.deleteYear());
        assertNull(sessions.created.deleteMonth());
        assertNull(sessions.created.deleteWeek());
        assertEquals("FY2025", sessions.created.deleteYearText());
        assertEquals("April", sessions.created.deleteMonthText());
        assertNull(sessions.created.sourceLoadSessionId());
        assertEquals(9001L, sessions.completedSessionId);
        assertEquals(1250, sessions.deletedRows);
        assertTrue(transaction.committed);
    }

    @Test
    void leadingZeroMonthIsPreservedForAuditAndRepository() {
        Transaction transaction = new Transaction();
        Sessions sessions = new Sessions(9004L);
        SalesByChannelDeletionRepository deletion = new SalesByChannelDeletionRepository() {
            @Override
            public int deleteByYearAndMonth(Connection connection, String year, String month) {
                assertEquals("2026", year);
                assertEquals("08", month);
                return 1;
            }
        };

        new SalesByChannelDeletionService(transaction.dataSource(), deletion, sessions)
                .deleteByYearAndMonth("2026", "08");

        assertEquals("2026", sessions.created.deleteYearText());
        assertEquals("08", sessions.created.deleteMonthText());
        assertNull(sessions.created.deleteYear());
        assertNull(sessions.created.deleteMonth());
    }

    @Test
    void loadSessionDeletionKeepsSourceSessionDistinctAndRecordsZeroAsSuccess() {
        long sourceSessionId = 10521L;
        Transaction transaction = new Transaction();
        Sessions sessions = new Sessions(9002L);
        SalesByChannelDeletionRepository deletion = new SalesByChannelDeletionRepository() {
            @Override
            public int deleteByLoadSessionId(Connection connection, long loadSessionId) {
                assertEquals(sourceSessionId, loadSessionId);
                return 0;
            }
        };

        DWHDataDeleteResult result = new SalesByChannelDeletionService(
                transaction.dataSource(), deletion, sessions
        ).deleteByLoadSessionId(sourceSessionId);

        assertEquals(0, result.deletedRows());
        assertEquals(DWHDeletionOperationMode.BY_LOAD_SESSION, sessions.created.operationMode());
        assertEquals(sourceSessionId, sessions.created.sourceLoadSessionId());
        assertTrue(sessions.newSessionId != sourceSessionId);
        assertEquals(0, sessions.deletedRows);
        assertEquals(0, sessions.errorCalls);
        assertTrue(transaction.committed);
    }

    @Test
    void failedYearMonthDeletionRollsBackAndFinishesNewSessionAsError() {
        Transaction transaction = new Transaction();
        Sessions sessions = new Sessions(9003L);
        SalesByChannelDeletionRepository deletion = new SalesByChannelDeletionRepository() {
            @Override
            public int deleteByYearAndMonth(Connection connection, String year, String month) {
                throw new RuntimeException("delete failed");
            }
        };

        RuntimeException failure = assertThrows(RuntimeException.class, () ->
                new SalesByChannelDeletionService(transaction.dataSource(), deletion, sessions)
                        .deleteByYearAndMonth("2026", "7")
        );

        assertEquals("delete failed", failure.getMessage());
        assertTrue(transaction.rolledBack);
        assertFalse(transaction.committed);
        assertEquals(9003L, sessions.errorSessionId);
        assertEquals("delete failed", sessions.errorMessage);
    }

    private static final class Sessions extends DWHDeletionSessionRepository {
        private final long newSessionId;
        private DWHDeletionSession created;
        private long completedSessionId;
        private long deletedRows = -1;
        private long errorSessionId;
        private String errorMessage;
        private int errorCalls;

        private Sessions(long newSessionId) {
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
    }

    private static final class Jdbc {
        private final int updateCount;
        private final List<String> calls = new ArrayList<>();
        private String sql;

        private Jdbc(int updateCount) {
            this.updateCount = updateCount;
        }

        private Connection connection() {
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("prepareStatement".equals(method.getName())) {
                            sql = (String) args[0];
                            return statement();
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        private PreparedStatement statement() {
            return (PreparedStatement) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> {
                        if (method.getName().startsWith("set")) {
                            calls.add(method.getName() + ":" + args[0] + ":" + args[1]);
                            return null;
                        }
                        if ("executeUpdate".equals(method.getName())) {
                            return updateCount;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }
}
