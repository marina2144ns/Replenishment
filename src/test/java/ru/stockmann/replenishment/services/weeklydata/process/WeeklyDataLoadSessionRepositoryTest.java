package ru.stockmann.replenishment.services.weeklydata.process;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyDataLoadSessionRepositoryTest {

    @Test
    void checksLoadSessionIdAndWeeklyDataLoadTypeCode() {
        RecordingPreparedStatement statement = new RecordingPreparedStatement();
        RecordingConnection connection = new RecordingConnection(statement);
        statement.resultSet = resultSet(true);

        boolean exists = new WeeklyDataLoadSessionRepository(dataSource(connection))
                .existsById(20L);

        assertTrue(exists);
        assertTrue(connection.sql.contains("FROM dbo.DWH_Excel_Load_Session"));
        assertTrue(connection.sql.contains("WHERE Id = ?"));
        assertTrue(connection.sql.contains("AND LoadTypeCode = ?"));
        assertEquals(20L, statement.values.get(1));
        assertEquals("WEEKLY_DATA", statement.values.get(2));
    }

    private static DataSource dataSource(RecordingConnection connection) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("getConnection".equals(method.getName())) {
                return connection.proxy();
            }
            return defaultValue(method.getReturnType());
        };
        return (DataSource) Proxy.newProxyInstance(
                DataSource.class.getClassLoader(),
                new Class<?>[]{DataSource.class},
                handler
        );
    }

    private static ResultSet resultSet(boolean hasRow) {
        class State {
            boolean consumed;
        }
        State state = new State();
        InvocationHandler handler = (proxy, method, args) -> {
            if ("next".equals(method.getName())) {
                boolean result = hasRow && !state.consumed;
                state.consumed = true;
                return result;
            }
            return defaultValue(method.getReturnType());
        };
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                handler
        );
    }

    private static final class RecordingPreparedStatement {
        private final Map<Integer, Object> values = new HashMap<>();
        private ResultSet resultSet;

        PreparedStatement proxy() {
            InvocationHandler handler = (proxy, method, args) -> {
                if ("setLong".equals(method.getName()) || "setString".equals(method.getName())) {
                    values.put((Integer) args[0], args[1]);
                    return null;
                }
                if ("executeQuery".equals(method.getName())) {
                    return resultSet;
                }
                return defaultValue(method.getReturnType());
            };
            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    handler
            );
        }
    }

    private static final class RecordingConnection {
        private final RecordingPreparedStatement statement;
        private String sql;

        private RecordingConnection(RecordingPreparedStatement statement) {
            this.statement = statement;
        }

        Connection proxy() {
            InvocationHandler handler = (proxy, method, args) -> {
                if ("prepareStatement".equals(method.getName())) {
                    sql = (String) args[0];
                    return statement.proxy();
                }
                return defaultValue(method.getReturnType());
            };
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    handler
            );
        }
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return null;
    }
}
