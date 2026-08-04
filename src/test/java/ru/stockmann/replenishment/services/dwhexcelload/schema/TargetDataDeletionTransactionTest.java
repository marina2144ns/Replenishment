package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.cddata.process.CDDataDeletionRepository;
import ru.stockmann.replenishment.services.cddata.process.CDDataDeletionService;
import ru.stockmann.replenishment.services.cdecom.process.CDEcomDeletionRepository;
import ru.stockmann.replenishment.services.cdecom.process.CDEcomDeletionService;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDataDeleteResult;
import ru.stockmann.replenishment.services.weeklydata.process.WeeklyDataDeletionRepository;
import ru.stockmann.replenishment.services.weeklydata.process.WeeklyDataDeletionService;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetDataDeletionTransactionTest {

    @Test
    void weeklyDataDeleteCommitsAndReturnsZeroWhenNothingMatched() {
        TransactionRecording transaction = new TransactionRecording();
        WeeklyDataDeletionRepository repository = new WeeklyDataDeletionRepository() {
            @Override
            public int deleteByPeriod(Connection connection, short year, short week) {
                return 0;
            }
        };

        DWHDataDeleteResult result = new WeeklyDataDeletionService(transaction.dataSource(), repository)
                .deleteByPeriod((short) 2026, (short) 31);

        assertEquals(0, result.deletedRows());
        transaction.assertCommitted();
    }

    @Test
    void cdDataDeleteCommitsAndReturnsAffectedRows() {
        TransactionRecording transaction = new TransactionRecording();
        CDDataDeletionRepository repository = new CDDataDeletionRepository() {
            @Override
            public int deleteByPeriod(Connection connection, int year, int week) {
                return 12;
            }
        };

        DWHDataDeleteResult result =
                new CDDataDeletionService(transaction.dataSource(), repository).deleteByPeriod(2026, 2);

        assertEquals(12, result.deletedRows());
        transaction.assertCommitted();
    }

    @Test
    void cdecomDeleteCommitsAndReturnsAffectedRows() {
        TransactionRecording transaction = new TransactionRecording();
        CDEcomDeletionRepository repository = new CDEcomDeletionRepository() {
            @Override
            public int deleteByLoadSessionId(Connection connection, long loadSessionId) {
                return 9;
            }
        };

        DWHDataDeleteResult result =
                new CDEcomDeletionService(transaction.dataSource(), repository).deleteByLoadSessionId(44L);

        assertEquals(9, result.deletedRows());
        transaction.assertCommitted();
    }

    private static final class TransactionRecording {
        private boolean autoCommit = true;
        private boolean committed;
        private boolean rolledBack;
        private boolean closed;

        private DataSource dataSource() {
            Connection connection = (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        return switch (method.getName()) {
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
                            case "close" -> {
                                closed = true;
                                yield null;
                            }
                            case "isClosed" -> closed;
                            default -> defaultValue(method.getReturnType());
                        };
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

        private void assertCommitted() {
            assertTrue(committed);
            assertFalse(rolledBack);
            assertTrue(autoCommit);
            assertTrue(closed);
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
