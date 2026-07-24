package ru.stockmann.replenishment.services.cdecom.process;

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

class CDEcomTargetRepositoryTest {

    @Test
    void publishesWithOneDeleteAndOneInsertSelect() {
        RecordingJdbc jdbc = new RecordingJdbc(7, 125);

        int publishedRows = new CDEcomTargetRepository().publishFromStage(jdbc.connection(), 10L);

        assertEquals(125, publishedRows);
        assertEquals(2, jdbc.sql.size());
        assertTrue(normalize(jdbc.sql.get(0)).contains(
                "delete from dbo.cd_ecom where loadsessionid = ?"
        ));
        String publishSql = normalize(jdbc.sql.get(1));
        assertTrue(publishSql.contains("insert into dbo.cd_ecom"));
        assertTrue(publishSql.contains("select loadsessionid, name, [year], season, [day], [data]"));
        assertTrue(publishSql.contains("from dbo.cd_ecom_stage where loadsessionid = ?"));
        assertTrue(publishSql.contains("skucommentbuyer"));
        assertTrue(publishSql.contains("skucollection"));
        assertFalse(publishSql.contains("select *"));
        assertFalse(publishSql.contains("excelrownum"));
        assertFalse(publishSql.contains("createdat"));
        assertFalse(publishSql.contains(" values "));
        assertEquals(List.of(10L, 10L), jdbc.loadSessionBindings);
        assertEquals(2, jdbc.executeUpdateCalls);
        assertEquals(0, jdbc.addBatchCalls);
        assertEquals(0, jdbc.executeBatchCalls);
    }

    @Test
    void deleteAndInsertFailuresArePropagated() {
        RecordingJdbc deleteFailure = new RecordingJdbc(0, 0);
        deleteFailure.failStatement = 1;
        assertThrows(RuntimeException.class, () ->
                new CDEcomTargetRepository().publishFromStage(deleteFailure.connection(), 10L));
        assertEquals(1, deleteFailure.sql.size());

        RecordingJdbc insertFailure = new RecordingJdbc(3, 0);
        insertFailure.failStatement = 2;
        assertThrows(RuntimeException.class, () ->
                new CDEcomTargetRepository().publishFromStage(insertFailure.connection(), 10L));
        assertEquals(2, insertFailure.sql.size());
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    private static final class RecordingJdbc {
        final int deleteCount;
        final int insertCount;
        final List<String> sql = new ArrayList<>();
        final List<Long> loadSessionBindings = new ArrayList<>();
        int executeUpdateCalls;
        int addBatchCalls;
        int executeBatchCalls;
        int failStatement;
        RecordingJdbc(int deleteCount, int insertCount) {
            this.deleteCount = deleteCount;
            this.insertCount = insertCount;
        }
        Connection connection() {
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("prepareStatement".equals(method.getName())) {
                            sql.add((String) args[0]);
                            return statement(sql.size());
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }
        PreparedStatement statement(int number) {
            return (PreparedStatement) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> {
                        if ("setLong".equals(method.getName())) {
                            loadSessionBindings.add((Long) args[1]);
                            return null;
                        }
                        if ("addBatch".equals(method.getName())) { addBatchCalls++; return null; }
                        if ("executeBatch".equals(method.getName())) {
                            executeBatchCalls++;
                            return new int[0];
                        }
                        if ("executeUpdate".equals(method.getName())) {
                            executeUpdateCalls++;
                            if (failStatement == number) throw new SQLException("failure");
                            return number == 1 ? deleteCount : insertCount;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (type == void.class || !type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        return 0;
    }
}
