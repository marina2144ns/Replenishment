package ru.stockmann.replenishment.services.salesbychannel.process;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesByChannelLoadSessionRepositoryTest {

    @Test
    void requiresExactSalesByChannelLoadTypeTogetherWithSessionId() {
        RecordingDataSource dataSource = new RecordingDataSource();

        assertTrue(new SalesByChannelLoadSessionRepository(dataSource).existsById(42L));

        assertTrue(dataSource.sql.contains("WHERE Id = ?"));
        assertTrue(dataSource.sql.contains("AND LoadTypeCode = ?"));
        assertEquals(42L, dataSource.parameters.get(1));
        assertEquals("SALES_BY_CHANNEL", dataSource.parameters.get(2));
    }

    private static final class RecordingDataSource implements DataSource {
        private final Map<Integer, Object> parameters = new HashMap<>();
        private String sql;

        @Override
        public Connection getConnection() {
            ResultSet resultSet = (ResultSet) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{ResultSet.class},
                    new java.lang.reflect.InvocationHandler() {
                        private boolean first = true;
                        @Override public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                            if ("next".equals(method.getName())) {
                                boolean result = first;
                                first = false;
                                return result;
                            }
                            return defaultValue(method.getReturnType());
                        }
                    });
            PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> {
                        if (method.getName().startsWith("set")) {
                            parameters.put((Integer) args[0], args[1]);
                            return null;
                        }
                        if ("executeQuery".equals(method.getName())) return resultSet;
                        return defaultValue(method.getReturnType());
                    });
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("prepareStatement".equals(method.getName())) {
                            sql = (String) args[0];
                            return statement;
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        @Override public Connection getConnection(String u, String p) { return getConnection(); }
        @Override public <T> T unwrap(Class<T> iface) { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
        @Override public java.io.PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(java.io.PrintWriter out) { }
        @Override public void setLoginTimeout(int seconds) { }
        @Override public int getLoginTimeout() { return 0; }
        @Override public java.util.logging.Logger getParentLogger() { return null; }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }
}
