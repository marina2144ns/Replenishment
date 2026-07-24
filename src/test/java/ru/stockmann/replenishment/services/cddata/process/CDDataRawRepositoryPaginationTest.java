package ru.stockmann.replenishment.services.cddata.process;

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

class CDDataRawRepositoryPaginationTest {

    @Test
    void readsFirstChunkFromInitialRawId() {
        QueryRecording jdbc = new QueryRecording(1_000);

        List<CDDataRawRow> rows = new CDDataRawRepository(null).findChunk(
                jdbc.connection(), 10L, CDDataRawRepository.INITIAL_LAST_RAW_ID, 1_000
        );

        assertEquals(1_000, rows.size());
        assertEquals(1_000, jdbc.parameters.get(1));
        assertEquals(10L, jdbc.parameters.get(2));
        assertEquals(0L, jdbc.parameters.get(3));
    }

    @Test
    void readsNextChunkAfterLastRawId() {
        QueryRecording jdbc = new QueryRecording(1_000);

        List<CDDataRawRow> rows =
                new CDDataRawRepository(null).findChunk(jdbc.connection(), 10L, 5_000L, 1_000);

        assertEquals(5_001L, rows.get(0).id());
        assertEquals(6_000L, rows.get(rows.size() - 1).id());
    }

    @Test
    void returnsIncompleteLastChunkAndThenEmptyChunk() {
        QueryRecording incomplete = new QueryRecording(237);
        QueryRecording empty = new QueryRecording(0);

        List<CDDataRawRow> rows =
                new CDDataRawRepository(null).findChunk(incomplete.connection(), 10L, 6_000L, 1_000);
        List<CDDataRawRow> noRows =
                new CDDataRawRepository(null).findChunk(empty.connection(), 10L, 6_237L, 1_000);

        assertEquals(237, rows.size());
        assertEquals(6_237L, rows.get(rows.size() - 1).id());
        assertTrue(noRows.isEmpty());
    }

    @Test
    void usesExplicitKeysetQueryWithoutOffsetOrSelectStar() {
        QueryRecording jdbc = new QueryRecording(0);

        new CDDataRawRepository(null).findChunk(jdbc.connection(), 10L, 0L, 1_000);

        String sql = jdbc.sql.replaceAll("\\s+", " ").toUpperCase();
        assertTrue(sql.contains("SELECT TOP (?)"));
        assertTrue(sql.contains("WHERE LOADSESSIONID = ? AND ID > ?"));
        assertTrue(sql.contains("ORDER BY ID"));
        assertTrue(sql.contains("EXCELROWNUM"));
        assertFalse(sql.contains("OFFSET"));
        assertFalse(sql.contains("SELECT *"));
    }

    private static final class QueryRecording {
        private final int rowCount;
        private final Map<Integer, Object> parameters = new HashMap<>();
        private String sql;

        private QueryRecording(int rowCount) {
            this.rowCount = rowCount;
        }

        Connection connection() {
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

        private ResultSet resultSet(long lastRawId) {
            class State {
                int row;
            }
            State state = new State();
            return (ResultSet) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "next" -> ++state.row <= rowCount;
                        case "getLong" -> {
                            String column = (String) args[0];
                            if ("Id".equals(column)) {
                                yield lastRawId + state.row;
                            }
                            if ("LoadSessionId".equals(column)) {
                                yield 10L;
                            }
                            yield lastRawId + state.row + 10L;
                        }
                        case "getString" -> null;
                        case "wasNull" -> false;
                        default -> defaultValue(method.getReturnType());
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
        if (type == long.class) {
            return 0L;
        }
        return 0;
    }
}
