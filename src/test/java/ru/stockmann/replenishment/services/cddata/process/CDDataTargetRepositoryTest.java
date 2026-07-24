package ru.stockmann.replenishment.services.cddata.process;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDDataTargetRepositoryTest {

    @Test
    void publishesWithOneDeleteAndOneSetBasedInsertSelect() {
        RecordingJdbc jdbc = new RecordingJdbc(7, 125);

        int publishedRows = new CDDataTargetRepository().publishFromStage(jdbc.connection(), 10L);

        assertEquals(125, publishedRows);
        assertEquals(2, jdbc.sql.size());
        assertTrue(normalize(jdbc.sql.get(0)).contains(
                "delete from dbo.cd_data where loadsessionid = ?"
        ));

        String publishSql = normalize(jdbc.sql.get(1));
        assertTrue(publishSql.contains("insert into dbo.cd_data"));
        assertTrue(publishSql.contains("select loadsessionid, nazvanie, god"));
        assertTrue(publishSql.contains("from dbo.cd_data_stage where loadsessionid = ?"));
        assertFalse(publishSql.contains("select *"));
        assertFalse(publishSql.contains("excelrownum"));
        assertFalse(publishSql.contains("createdat"));
        assertFalse(publishSql.contains(" values "));
        assertFalse(publishSql.contains("addbatch"));
        assertEquals(List.of(10L, 10L), jdbc.loadSessionBindings);
        assertEquals(2, jdbc.executeUpdateCalls);
        assertEquals(0, jdbc.addBatchCalls);
        assertEquals(0, jdbc.executeBatchCalls);
    }

    @Test
    void deleteFailureStopsBeforeInsert() {
        RecordingJdbc jdbc = new RecordingJdbc(0, 0);
        jdbc.failStatement = 1;

        assertThrows(RuntimeException.class, () ->
                new CDDataTargetRepository().publishFromStage(jdbc.connection(), 10L));

        assertEquals(1, jdbc.sql.size());
        assertEquals(1, jdbc.executeUpdateCalls);
    }

    @Test
    void insertSelectFailureIsPropagated() {
        RecordingJdbc jdbc = new RecordingJdbc(3, 0);
        jdbc.failStatement = 2;

        assertThrows(RuntimeException.class, () ->
                new CDDataTargetRepository().publishFromStage(jdbc.connection(), 10L));

        assertEquals(2, jdbc.sql.size());
        assertEquals(2, jdbc.executeUpdateCalls);
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    private static final class RecordingJdbc {
        private final int deleteCount;
        private final int insertCount;
        private final List<String> sql = new ArrayList<>();
        private final List<Long> loadSessionBindings = new ArrayList<>();
        private int executeUpdateCalls;
        private int addBatchCalls;
        private int executeBatchCalls;
        private int failStatement;

        private RecordingJdbc(int deleteCount, int insertCount) {
            this.deleteCount = deleteCount;
            this.insertCount = insertCount;
        }

        private Connection connection() {
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("prepareStatement".equals(method.getName())) {
                            sql.add((String) args[0]);
                            return statement(sql.size());
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        private PreparedStatement statement(int statementNumber) {
            return (PreparedStatement) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> {
                        if ("setLong".equals(method.getName())) {
                            loadSessionBindings.add((Long) args[1]);
                            return null;
                        }
                        if ("addBatch".equals(method.getName())) {
                            addBatchCalls++;
                            return null;
                        }
                        if ("executeBatch".equals(method.getName())) {
                            executeBatchCalls++;
                            return new int[0];
                        }
                        if ("executeUpdate".equals(method.getName())) {
                            executeUpdateCalls++;
                            if (statementNumber == failStatement) {
                                throw new SQLException("statement failed");
                            }
                            return statementNumber == 1 ? deleteCount : insertCount;
                        }
                        return defaultValue(method.getReturnType());
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
        return 0;
    }
}
