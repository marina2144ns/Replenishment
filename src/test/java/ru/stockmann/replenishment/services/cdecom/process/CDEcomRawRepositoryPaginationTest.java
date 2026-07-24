package ru.stockmann.replenishment.services.cdecom.process;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDEcomRawRepositoryPaginationTest {

    @Test
    void readsFirstAndNextChunksUsingLastRawId() {
        QueryRecording first = new QueryRecording(1_000);
        QueryRecording next = new QueryRecording(1_000);

        List<CDEcomRawRow> firstRows = repository().findChunk(
                first.connection(), 10L, CDEcomRawRepository.INITIAL_LAST_RAW_ID, 1_000
        );
        List<CDEcomRawRow> nextRows = repository().findChunk(next.connection(), 10L, 5_000L, 1_000);

        assertEquals(1_000, firstRows.size());
        assertEquals(1L, firstRows.get(0).id());
        assertEquals(5_001L, nextRows.get(0).id());
        assertEquals(6_000L, nextRows.get(999).id());
        assertEquals(1_000, first.parameters.get(1));
        assertEquals(10L, first.parameters.get(2));
        assertEquals(0L, first.parameters.get(3));
    }

    @Test
    void returnsIncompleteLastChunkAndEmptyChunk() {
        QueryRecording incomplete = new QueryRecording(237);
        QueryRecording empty = new QueryRecording(0);

        List<CDEcomRawRow> rows = repository().findChunk(incomplete.connection(), 10L, 6_000L, 1_000);
        List<CDEcomRawRow> noRows = repository().findChunk(empty.connection(), 10L, 6_237L, 1_000);

        assertEquals(237, rows.size());
        assertEquals(6_237L, rows.get(236).id());
        assertTrue(noRows.isEmpty());
    }

    @Test
    void queryUsesExplicitColumnsAndKeysetOrdering() {
        QueryRecording jdbc = new QueryRecording(0);

        repository().findChunk(jdbc.connection(), 10L, 0L, 1_000);

        String sql = jdbc.sql.replaceAll("\\s+", " ").toUpperCase();
        assertTrue(sql.contains("SELECT TOP (?)"));
        assertTrue(sql.contains("WHERE LOADSESSIONID = ? AND ID > ?"));
        assertTrue(sql.contains("ORDER BY ID"));
        assertTrue(sql.contains("EXCELROWNUM"));
        assertFalse(sql.contains("OFFSET"));
        assertFalse(sql.contains("SELECT *"));
    }

    private CDEcomRawRepository repository() {
        return new CDEcomRawRepository(null);
    }

    private static final class QueryRecording {
        final int rowCount;
        final Map<Integer, Object> parameters = new HashMap<>();
        String sql;
        QueryRecording(int rowCount) { this.rowCount = rowCount; }

        Connection connection() {
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("prepareStatement".equals(method.getName())) {
                            sql = (String) args[0];
                            return statement();
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        PreparedStatement statement() {
            return (PreparedStatement) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> {
                        if ("setInt".equals(method.getName()) || "setLong".equals(method.getName())) {
                            parameters.put((Integer) args[0], args[1]);
                            return null;
                        }
                        if ("executeQuery".equals(method.getName())) {
                            return resultSet((Long) parameters.get(3));
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        ResultSet resultSet(long lastRawId) {
            class State { int row; boolean wasNull; }
            State state = new State();
            return (ResultSet) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> {
                        if ("next".equals(method.getName())) return ++state.row <= rowCount;
                        if ("getLong".equals(method.getName())) {
                            String column = (String) args[0];
                            state.wasNull = false;
                            if ("Id".equals(column)) return lastRawId + state.row;
                            if ("LoadSessionId".equals(column)) return 10L;
                            if ("ExcelRowNum".equals(column)) return (long) state.row;
                            return 0L;
                        }
                        if ("wasNull".equals(method.getName())) return state.wasNull;
                        if ("getString".equals(method.getName())) return null;
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
