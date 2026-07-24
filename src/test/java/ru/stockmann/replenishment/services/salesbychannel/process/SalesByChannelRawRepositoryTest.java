package ru.stockmann.replenishment.services.salesbychannel.process;

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

class SalesByChannelRawRepositoryTest {

    @Test
    void usesExplicitKeysetQueryAndConfiguredChunkSize() {
        Jdbc jdbc = new Jdbc(List.of(7L, 11L, 25L));
        SalesByChannelRawRepository repository = new SalesByChannelRawRepository(null, 1_000);

        List<SalesByChannelRawRow> rows = repository.findChunk(jdbc.connection(), 10L, 5L, 1_000);

        assertEquals(List.of(7L, 11L, 25L), rows.stream().map(SalesByChannelRawRow::id).toList());
        assertEquals(1_000, jdbc.parameters.get(1));
        assertEquals(10L, jdbc.parameters.get(2));
        assertEquals(5L, jdbc.parameters.get(3));
        String normalized = jdbc.sql.replaceAll("\\s+", " ").toUpperCase();
        assertTrue(normalized.contains("SELECT TOP (?)"));
        assertTrue(normalized.contains("AND ID > ?"));
        assertTrue(normalized.contains("ORDER BY ID"));
        assertTrue(normalized.contains("EXCELROWNUM"));
        assertFalse(normalized.contains("OFFSET"));
        assertFalse(normalized.contains("SELECT *"));
        assertEquals(1_000, SalesByChannelProcessConfiguration.DEFAULT_CHUNK_SIZE);
    }

    @Test
    void emptyChunkTerminatesNaturally() {
        assertTrue(new SalesByChannelRawRepository(null, 1_000)
                .findChunk(new Jdbc(List.of()).connection(), 10L, 25L, 1_000).isEmpty());
    }

    private static final class Jdbc {
        private final List<Long> ids;
        private final Map<Integer, Object> parameters = new HashMap<>();
        private String sql;

        private Jdbc(List<Long> ids) {
            this.ids = ids;
        }

        Connection connection() {
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                        if ("prepareStatement".equals(method.getName())) {
                            sql = (String) args[0];
                            return statement();
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        PreparedStatement statement() {
            return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{PreparedStatement.class}, (proxy, method, args) -> {
                        if (method.getName().startsWith("set")) {
                            parameters.put((Integer) args[0], args[1]);
                            return null;
                        }
                        if ("executeQuery".equals(method.getName())) return resultSet();
                        return defaultValue(method.getReturnType());
                    });
        }

        ResultSet resultSet() {
            class State { int index = -1; boolean wasNull; }
            State state = new State();
            return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{ResultSet.class}, (proxy, method, args) -> {
                        if ("next".equals(method.getName())) return ++state.index < ids.size();
                        if ("getLong".equals(method.getName())) {
                            String column = (String) args[0];
                            state.wasNull = false;
                            if ("Id".equals(column)) return ids.get(state.index);
                            if ("LoadSessionId".equals(column)) return 10L;
                            if ("ExcelRowNum".equals(column)) return 100L + state.index;
                        }
                        if ("wasNull".equals(method.getName())) return state.wasNull;
                        if ("getString".equals(method.getName())) {
                            String column = (String) args[0];
                            if ("year".equals(column)) return "2025";
                            if ("month".equals(column)) return "April";
                            return null;
                        }
                        return defaultValue(method.getReturnType());
                    });
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
