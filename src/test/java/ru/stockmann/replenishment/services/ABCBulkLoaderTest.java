package ru.stockmann.replenishment.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ABCBulkLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void instrumentationPreservesSourceMappingProcedureModeAndResult() throws Exception {
        RecordingDatabase database = new RecordingDatabase();
        Path file = tempDir.resolve("abc.csv");
        Files.writeString(file,
                "header\n"
                        + "tm;section;department;subgroup;sku-1;1 234;56%;A;B;31.01.2026\n",
                StandardCharsets.UTF_8);

        ABCBulkLoader.LoadResult result = new ABCBulkLoader(database.dataSource())
                .bulkLoad(file.toString(), "6R");

        assertEquals("OK", result.status());
        assertEquals(1, result.stagedRows());
        assertEquals(List.of(), result.errors());
        assertEquals("6R", database.procedureMode);
        assertEquals(2, database.commits);
        assertEquals(1, database.rows.size());
        Map<Integer, String> row = database.rows.get(0);
        assertEquals("tm", row.get(1));
        assertEquals("section", row.get(2));
        assertEquals("department", row.get(3));
        assertEquals("subgroup", row.get(4));
        assertEquals("sku-1", row.get(5));
        assertEquals("1234", row.get(6));
        assertEquals("56", row.get(7));
        assertEquals("A", row.get(8));
        assertEquals("B", row.get(9));
        assertEquals("31.01.2026", row.get(10));
        assertNull(database.deleteFallbackSql);
    }

    private static final class RecordingDatabase {
        private final List<Map<Integer, String>> rows = new ArrayList<>();
        private int commits;
        private String procedureMode;
        private String deleteFallbackSql;

        private DataSource dataSource() {
            return proxy(DataSource.class, (proxy, method, args) -> switch (method.getName()) {
                case "getConnection" -> connection();
                default -> defaultValue(method.getReturnType());
            });
        }

        private Connection connection() {
            return proxy(Connection.class, (proxy, method, args) -> switch (method.getName()) {
                case "prepareStatement" -> ((String) args[0]).startsWith("SELECT 1")
                        ? stageCheckStatement() : insertStatement();
                case "createStatement" -> clearStatement();
                case "prepareCall" -> mergeStatement();
                case "commit" -> {
                    commits++;
                    yield null;
                }
                default -> defaultValue(method.getReturnType());
            });
        }

        private PreparedStatement stageCheckStatement() {
            return proxy(PreparedStatement.class, (proxy, method, args) -> switch (method.getName()) {
                case "executeQuery" -> proxy(ResultSet.class, new InvocationHandler() {
                    private boolean first = true;

                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        if (method.getName().equals("next")) {
                            boolean result = first;
                            first = false;
                            return result;
                        }
                        return defaultValue(method.getReturnType());
                    }
                });
                default -> defaultValue(method.getReturnType());
            });
        }

        private Statement clearStatement() {
            return proxy(Statement.class, (proxy, method, args) -> switch (method.getName()) {
                case "execute" -> true;
                case "executeUpdate" -> {
                    deleteFallbackSql = (String) args[0];
                    yield 0;
                }
                default -> defaultValue(method.getReturnType());
            });
        }

        private PreparedStatement insertStatement() {
            return proxy(PreparedStatement.class, new InvocationHandler() {
                private final Map<Integer, String> current = new HashMap<>();
                private final List<Map<Integer, String>> batch = new ArrayList<>();

                @Override
                public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                    return switch (method.getName()) {
                        case "setString" -> {
                            current.put((Integer) args[0], (String) args[1]);
                            yield null;
                        }
                        case "addBatch" -> {
                            batch.add(new HashMap<>(current));
                            yield null;
                        }
                        case "executeBatch" -> {
                            rows.addAll(batch);
                            int[] result = new int[batch.size()];
                            Arrays.fill(result, 1);
                            batch.clear();
                            yield result;
                        }
                        default -> defaultValue(method.getReturnType());
                    };
                }
            });
        }

        private CallableStatement mergeStatement() {
            return proxy(CallableStatement.class, (proxy, method, args) -> switch (method.getName()) {
                case "setString" -> {
                    procedureMode = (String) args[1];
                    yield null;
                }
                case "execute" -> true;
                default -> defaultValue(method.getReturnType());
            });
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }
}
